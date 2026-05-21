package com.oaiss.chain.service;

import com.oaiss.chain.dto.TradeRequest;
import com.oaiss.chain.dto.TradeResponse;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.entity.Transaction;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.enums.TradeStatusEnum;
import com.oaiss.chain.enums.TradeTypeEnum;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.annotation.DistributedLock;
import com.oaiss.chain.exception.TradeException;
import com.oaiss.chain.repository.EnterpriseRepository;
import com.oaiss.chain.repository.TransactionRepository;
import com.oaiss.chain.repository.UserRepository;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 碳交易服务
 * 
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final TransactionRepository transactionRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final UserRepository userRepository;
    private final MetricsService metricsService;

    /**
     * 创建交易（P2P）
     */
    @DistributedLock(key = "'trade:seller:' + #currentUser.userId", expireTime = 30)
    @Transactional
    public TradeResponse createP2PTrade(JwtUserDetails currentUser, TradeRequest request) {
        if (!TradeTypeEnum.P2P.getCode().equals(request.getTradeType())) {
            throw TradeException.tradeNotFound(0L); // 不匹配类型
        }

        Long sellerId = request.getSellerId();
        Long buyerId = request.getBuyerId();

        // 验证双方不能相同
        if (sellerId.equals(buyerId)) {
            throw TradeException.samePartyError(sellerId);
        }

        // 验证当前用户必须是卖方（防止越权）
        if (!currentUser.getUserId().equals(sellerId)) {
            throw TradeException.unauthorizedTrade(currentUser.getUserId(), sellerId);
        }

        // 验证卖方配额
        Enterprise sellerEnterprise = enterpriseRepository.findByUserId(sellerId)
                .orElseThrow(() -> TradeException.insufficientQuota(BigDecimal.ZERO, request.getQuantity()));
        
        if (sellerEnterprise.getCarbonTradable().compareTo(request.getQuantity()) < 0) {
            throw TradeException.insufficientQuota(sellerEnterprise.getCarbonTradable(), request.getQuantity());
        }

        // 创建交易记录
        BigDecimal totalAmount = request.getQuantity().multiply(request.getUnitPrice());

        Transaction trade = Transaction.builder()
                .tradeNo(CommonUtils.generateTradeId())
                .tradeType(request.getTradeType())
                .sellerId(sellerId)
                .buyerId(buyerId)
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(totalAmount)
                .reportId(request.getReportId())
                .status(TradeStatusEnum.PENDING.getCode())
                .remark(request.getRemark())
                .build();

        trade = transactionRepository.save(trade);

        log.info("P2P trade created: {} ({} -> {}, qty: {}, price: {})", 
                trade.getTradeNo(), sellerId, buyerId, request.getQuantity(), request.getUnitPrice());

        return toResponse(trade);
    }

    /**
     * 创建拍卖挂单
     */
    @Transactional
    public TradeResponse createAuctionOrder(JwtUserDetails currentUser, TradeRequest request) {
        Enterprise enterprise = enterpriseRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> TradeException.insufficientQuota(BigDecimal.ZERO, request.getQuantity()));

        if (enterprise.getCarbonTradable().compareTo(request.getQuantity()) < 0) {
            throw TradeException.insufficientQuota(enterprise.getCarbonTradable(), request.getQuantity());
        }

        BigDecimal totalAmount = request.getQuantity().multiply(request.getUnitPrice());

        Transaction trade = Transaction.builder()
                .tradeNo(CommonUtils.generateTradeId())
                .tradeType(TradeTypeEnum.AUCTION.getCode())
                .sellerId(currentUser.getUserId())
                .buyerId(0L) // 拍卖挂单，买方待定
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(totalAmount)
                .status(TradeStatusEnum.PENDING.getCode())
                .remark(request.getRemark())
                .build();

        trade = transactionRepository.save(trade);

        log.info("Auction order created: {} by user {}", trade.getTradeNo(), currentUser.getUsername());
        return toResponse(trade);
    }

    /**
     * 确认交易
     * 安全措施：
     * 1. 仅允许PENDING状态的交易确认（防止并发重复确认）
     * 2. 立即转移为PROCESSING状态，防止竞态条件
     */
    @Transactional
    public TradeResponse confirmTrade(Long tradeId, Long currentUserId) {
        Transaction trade = transactionRepository.findById(tradeId)
                .orElseThrow(() -> TradeException.tradeNotFound(tradeId));

        if (!trade.getSellerId().equals(currentUserId) && !trade.getBuyerId().equals(currentUserId)) {
            throw TradeException.tradeNotFound(tradeId);
        }

        TradeStatusEnum status = TradeStatusEnum.fromCode(trade.getStatus());
        // 严格检查：仅PENDING状态允许确认（防止并发确认和重复确认）
        if (status != TradeStatusEnum.PENDING) {
            throw TradeException.orderCompleted(tradeId);
        }

        // 立即将状态设置为PROCESSING并保存，防止并发请求重复操作
        trade.setStatus(TradeStatusEnum.PROCESSING.getCode());
        transactionRepository.save(trade);

        // 更新配额
        final BigDecimal tradeQuantity = trade.getQuantity();
        Enterprise sellerEnterprise = enterpriseRepository.findByUserId(trade.getSellerId())
                .orElseThrow(() -> TradeException.insufficientQuota(BigDecimal.ZERO, tradeQuantity));
        Enterprise buyerEnterprise = enterpriseRepository.findByUserId(trade.getBuyerId())
                .orElseThrow(() -> TradeException.tradeNotFound(tradeId));

        // 卖方减少配额
        sellerEnterprise.setCarbonTradable(sellerEnterprise.getCarbonTradable().subtract(trade.getQuantity()));
        sellerEnterprise.setCarbonUsed(sellerEnterprise.getCarbonUsed().add(trade.getQuantity()));

        // 买方增加配额
        buyerEnterprise.setCarbonQuota(buyerEnterprise.getCarbonQuota().add(trade.getQuantity()));
        buyerEnterprise.setCarbonTradable(buyerEnterprise.getCarbonTradable().add(trade.getQuantity()));

        enterpriseRepository.save(sellerEnterprise);
        enterpriseRepository.save(buyerEnterprise);

        // 更新交易状态
        trade.setStatus(TradeStatusEnum.COMPLETED.getCode());
        trade.setCompletedAt(LocalDateTime.now());
        trade = transactionRepository.save(trade);

        log.info("Trade completed: {}", trade.getTradeNo());
        return toResponse(trade);
    }

    /**
     * 取消交易
     */
    @Transactional
    public TradeResponse cancelTrade(Long tradeId, JwtUserDetails currentUser) {
        Transaction trade = transactionRepository.findById(tradeId)
                .orElseThrow(() -> TradeException.tradeNotFound(tradeId));

        TradeStatusEnum status = TradeStatusEnum.fromCode(trade.getStatus());
        if (!status.isCancellable()) {
            throw TradeException.orderCancelled(tradeId);
        }

        trade.setStatus(TradeStatusEnum.CANCELLED.getCode());
        trade = transactionRepository.save(trade);

        log.info("Trade cancelled: {} by user {}", trade.getTradeNo(), currentUser.getUsername());
        return toResponse(trade);
    }

    /**
     * 获取交易详情（含权限校验）
     */
    @Transactional(readOnly = true)
    public TradeResponse getTrade(Long tradeId, JwtUserDetails currentUser) {
        Transaction trade = transactionRepository.findById(tradeId)
                .orElseThrow(() -> TradeException.tradeNotFound(tradeId));

        // 权限校验：管理员/审核员/第三方可查看所有，企业用户只能查看自己参与的交易
        boolean isParticipant = trade.getSellerId().equals(currentUser.getUserId())
                || trade.getBuyerId().equals(currentUser.getUserId());
        if (!currentUser.isAdmin() && !currentUser.isReviewer() && !currentUser.hasRole("THIRD_PARTY") && !isParticipant) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权查看此交易详情");
        }

        return toResponse(trade);
    }

    /**
     * 分页查询交易
     */
    public Page<TradeResponse> listTrades(Long sellerId, Long buyerId,
            Integer tradeType, Integer status, Integer page, Integer size) {
        size = CommonUtils.sanitizePageSize(size);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> trades = transactionRepository.search(sellerId, buyerId, tradeType, status, pageable);
        Map<Long, String> userNames = resolveUserNames(trades.getContent());
        return trades.map(t -> toResponse(t, userNames));
    }

    /**
     * 查询我的交易（作为买方或卖方）
     */
    public Page<TradeResponse> listMyTrades(JwtUserDetails currentUser,
            Integer tradeType, Integer status, Integer page, Integer size) {
        size = CommonUtils.sanitizePageSize(size);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Long userId = currentUser.getUserId();
        Page<Transaction> trades = transactionRepository.findByUserIdRelated(userId, tradeType, status, pageable);
        Map<Long, String> userNames = resolveUserNames(trades.getContent());
        return trades.map(t -> toResponse(t, userNames));
    }

    // ==================== 私有方法 ====================

    /**
     * 批量解析交易涉及的用户名称（解决 N+1 查询问题）
     */
    private Map<Long, String> resolveUserNames(java.util.List<Transaction> trades) {
        Set<Long> userIds = new HashSet<>();
        for (Transaction t : trades) {
            userIds.add(t.getSellerId());
            if (t.getBuyerId() > 0) {
                userIds.add(t.getBuyerId());
            }
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));
    }

    private TradeResponse toResponse(Transaction trade) {
        String sellerName = userRepository.findById(trade.getSellerId())
                .map(User::getRealName).orElse("未知");
        String buyerName = trade.getBuyerId() > 0
                ? userRepository.findById(trade.getBuyerId()).map(User::getRealName).orElse("未知")
                : "待定";

        return buildResponse(trade, sellerName, buyerName);
    }

    private TradeResponse toResponse(Transaction trade, Map<Long, String> userNames) {
        String sellerName = userNames.getOrDefault(trade.getSellerId(), "未知");
        String buyerName = trade.getBuyerId() > 0
                ? userNames.getOrDefault(trade.getBuyerId(), "未知")
                : "待定";

        return buildResponse(trade, sellerName, buyerName);
    }

    private TradeResponse buildResponse(Transaction trade, String sellerName, String buyerName) {
        return TradeResponse.builder()
                .id(trade.getId())
                .tradeNo(trade.getTradeNo())
                .tradeType(trade.getTradeType())
                .tradeTypeText(safeTradeTypeText(trade.getTradeType()))
                .sellerId(trade.getSellerId())
                .sellerName(sellerName)
                .buyerId(trade.getBuyerId())
                .buyerName(buyerName)
                .quantity(trade.getQuantity())
                .unitPrice(trade.getUnitPrice())
                .totalAmount(trade.getTotalAmount())
                .reportId(trade.getReportId())
                .status(trade.getStatus())
                .statusText(safeTradeStatusText(trade.getStatus()))
                .remark(trade.getRemark())
                .blockchainTxHash(trade.getBlockchainTxHash())
                .completedAt(trade.getCompletedAt())
                .createdAt(trade.getCreatedAt())
                .build();
    }

    private String safeTradeTypeText(Integer code) {
        try {
            TradeTypeEnum e = TradeTypeEnum.fromCode(code);
            return e != null ? e.getDescription() : "未知";
        } catch (IllegalArgumentException ex) {
            return "未知";
        }
    }

    private String safeTradeStatusText(Integer code) {
        try {
            TradeStatusEnum e = TradeStatusEnum.fromCode(code);
            return e != null ? e.getDescription() : "未知";
        } catch (IllegalArgumentException ex) {
            return "未知";
        }
    }
}
