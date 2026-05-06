package com.oaiss.chain.service;

import com.oaiss.chain.dto.AuctionOrderRequest;
import com.oaiss.chain.dto.AuctionOrderResponse;
import com.oaiss.chain.dto.MatchingResultResponse;
import com.oaiss.chain.entity.AuctionOrder;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.entity.MatchingResult;
import com.oaiss.chain.entity.Transaction;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.enums.AuctionOrderStatusEnum;
import com.oaiss.chain.enums.MatchingStatusEnum;
import com.oaiss.chain.enums.TradeStatusEnum;
import com.oaiss.chain.enums.TradeTypeEnum;
import com.oaiss.chain.exception.TradeException;
import com.oaiss.chain.repository.AuctionOrderRepository;
import com.oaiss.chain.repository.EnterpriseRepository;
import com.oaiss.chain.repository.MatchingResultRepository;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 双向拍卖服务
 * 实现碳配额的双向拍卖撮合算法
 * <p>
 * 算法说明：
 * 1. 买方按期望价格降序排列（出价高的优先）
 * 2. 卖方按期望价格升序排列（出价低的优先）
 * 3. 当最高买价 >= 最低卖价时，撮合成功
 * 4. 成交价格 = (买方价格 + 卖方价格) / 2
 * 5. 支持部分匹配
 *
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoubleAuctionService {

    private final AuctionOrderRepository auctionOrderRepository;
    private final MatchingResultRepository matchingResultRepository;
    private final TransactionRepository transactionRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final UserRepository userRepository;

    private static final ThreadLocalRandom MATCH_RNG = ThreadLocalRandom.current();

    /**
     * 提交买入挂单
     */
    @Transactional
    public AuctionOrderResponse placeBuyOrder(JwtUserDetails currentUser, AuctionOrderRequest request) {
        Enterprise enterprise = enterpriseRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> TradeException.insufficientQuota(BigDecimal.ZERO, request.getQuantity()));

        // 验证可交易配额
        if (enterprise.getCarbonTradable().compareTo(request.getQuantity()) < 0) {
            throw TradeException.insufficientQuota(enterprise.getCarbonTradable(), request.getQuantity());
        }

        AuctionOrder order = AuctionOrder.builder()
                .orderNo(generateOrderNo("B"))
                .userId(currentUser.getUserId())
                .direction(1)
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .matchedQuantity(BigDecimal.ZERO)
                .status(AuctionOrderStatusEnum.PENDING.getCode())
                .build();

        order = auctionOrderRepository.save(order);
        log.info("Buy order placed: {} by user {}, qty={}, price={}",
                order.getOrderNo(), currentUser.getUsername(), request.getQuantity(), request.getPrice());

        return toOrderResponse(order);
    }

    /**
     * 提交卖出挂单
     */
    @Transactional
    public AuctionOrderResponse placeSellOrder(JwtUserDetails currentUser, AuctionOrderRequest request) {
        Enterprise enterprise = enterpriseRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> TradeException.insufficientQuota(BigDecimal.ZERO, request.getQuantity()));

        // 验证可交易配额
        BigDecimal remaining = enterprise.getCarbonTradable();
        if (remaining.compareTo(request.getQuantity()) < 0) {
            throw TradeException.insufficientQuota(remaining, request.getQuantity());
        }

        AuctionOrder order = AuctionOrder.builder()
                .orderNo(generateOrderNo("S"))
                .userId(currentUser.getUserId())
                .direction(2)
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .matchedQuantity(BigDecimal.ZERO)
                .status(AuctionOrderStatusEnum.PENDING.getCode())
                .build();

        order = auctionOrderRepository.save(order);
        log.info("Sell order placed: {} by user {}, qty={}, price={}",
                order.getOrderNo(), currentUser.getUsername(), request.getQuantity(), request.getPrice());

        return toOrderResponse(order);
    }

    /**
     * 执行双向拍卖撮合
     * <p>
     * 核心算法：
     * 1. 取出所有待匹配/部分匹配的买单（按价格降序）
     * 2. 取出所有待匹配/部分匹配的卖单（按价格升序）
     * 3. 双指针匹配：买价 >= 卖价时撮合
     * 4. 成交价 = (买价 + 卖价) / 2
     * 5. 创建撮合记录 + 交易记录
     * 6. 更新双方企业配额
     *
     * @return 撮合结果列表
     */
    @Transactional
    public synchronized List<MatchingResultResponse> executeMatching() {
        List<Integer> activeStatuses = Arrays.asList(
                AuctionOrderStatusEnum.PENDING.getCode(),
                AuctionOrderStatusEnum.PARTIALLY_MATCHED.getCode()
        );

        // 买方：价格降序（出价高的优先）
        List<AuctionOrder> buyOrders = auctionOrderRepository
                .findByDirectionAndStatusInAndDeletedFalseOrderByPriceDesc(1, activeStatuses);

        // 卖方：价格升序（出价低的优先）
        List<AuctionOrder> sellOrders = auctionOrderRepository
                .findByDirectionAndStatusInAndDeletedFalseOrderByPriceAsc(2, activeStatuses);

        List<MatchingResultResponse> results = new ArrayList<>();

        if (buyOrders.isEmpty() || sellOrders.isEmpty()) {
            log.info("No matching opportunity: buy={}, sell={}", buyOrders.size(), sellOrders.size());
            return results;
        }

        int sellIdx = 0;
        for (AuctionOrder buyOrder : buyOrders) {
            BigDecimal buyRemaining = buyOrder.getQuantity().subtract(buyOrder.getMatchedQuantity());

            while (buyRemaining.compareTo(BigDecimal.ZERO) > 0 && sellIdx < sellOrders.size()) {
                AuctionOrder sellOrder = sellOrders.get(sellIdx);
                BigDecimal sellRemaining = sellOrder.getQuantity().subtract(sellOrder.getMatchedQuantity());

                if (sellRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    sellIdx++;
                    continue;
                }

                // 检查价格是否匹配：买方价格 >= 卖方价格
                if (buyOrder.getPrice().compareTo(sellOrder.getPrice()) < 0) {
                    break; // 后续买方价格更低，无法匹配
                }

                // 计算匹配数量
                BigDecimal matchQty = buyRemaining.min(sellRemaining);

                // 计算成交价格：(买方价格 + 卖方价格) / 2
                BigDecimal settlementPrice = buyOrder.getPrice()
                        .add(sellOrder.getPrice())
                        .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

                BigDecimal totalAmount = matchQty.multiply(settlementPrice)
                        .setScale(2, RoundingMode.HALF_UP);

                // 创建撮合记录
                MatchingResult match = MatchingResult.builder()
                        .matchNo(generateMatchNo())
                        .buyOrderId(buyOrder.getId())
                        .sellOrderId(sellOrder.getId())
                        .buyerId(buyOrder.getUserId())
                        .sellerId(sellOrder.getUserId())
                        .matchedQuantity(matchQty)
                        .settlementPrice(settlementPrice)
                        .totalAmount(totalAmount)
                        .status(MatchingStatusEnum.PENDING_SETTLEMENT.getCode())
                        .build();

                match = matchingResultRepository.save(match);

                // 创建交易记录
                Transaction trade = Transaction.builder()
                        .tradeNo(CommonUtils.generateTradeId())
                        .tradeType(TradeTypeEnum.AUCTION.getCode())
                        .sellerId(sellOrder.getUserId())
                        .buyerId(buyOrder.getUserId())
                        .quantity(matchQty)
                        .unitPrice(settlementPrice)
                        .totalAmount(totalAmount)
                        .status(TradeStatusEnum.COMPLETED.getCode())
                        .completedAt(LocalDateTime.now())
                        .build();
                trade = transactionRepository.save(trade);

                // 更新撮合记录关联交易
                match.setTransactionId(trade.getId());
                match.setStatus(MatchingStatusEnum.SETTLED.getCode());
                match.setSettledAt(LocalDateTime.now());
                matchingResultRepository.save(match);

                // 更新买方挂单
                buyOrder.setMatchedQuantity(buyOrder.getMatchedQuantity().add(matchQty));
                buyOrder.setSettlementPrice(settlementPrice);
                updateOrderStatus(buyOrder);

                // 更新卖方挂单
                sellOrder.setMatchedQuantity(sellOrder.getMatchedQuantity().add(matchQty));
                sellOrder.setSettlementPrice(settlementPrice);
                updateOrderStatus(sellOrder);

                // 更新企业配额
                updateEnterpriseQuota(buyOrder.getUserId(), sellOrder.getUserId(), matchQty);

                buyRemaining = buyRemaining.subtract(matchQty);

                results.add(toMatchResponse(match));

                log.info("Matched: buy={}, sell={}, qty={}, price={}, total={}",
                        buyOrder.getOrderNo(), sellOrder.getOrderNo(), matchQty, settlementPrice, totalAmount);
            }

            auctionOrderRepository.save(buyOrder);
        }

        // 保存所有卖单更新
        for (int i = 0; i < sellIdx && i < sellOrders.size(); i++) {
            auctionOrderRepository.save(sellOrders.get(i));
        }
        // 保存剩余被部分匹配的卖单
        for (int i = sellIdx; i < sellOrders.size(); i++) {
            AuctionOrder so = sellOrders.get(i);
            if (so.getMatchedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                auctionOrderRepository.save(so);
            }
        }

        log.info("Matching round completed: {} matches", results.size());
        return results;
    }

    /**
     * 分页查询挂单
     */
    public Page<AuctionOrderResponse> listOrders(Integer direction, Integer status,
                                                   Integer page, Integer size) {
        size = CommonUtils.sanitizePageSize(size);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuctionOrder> orders;
        if (direction != null && status != null) {
            orders = auctionOrderRepository.findByDirectionAndStatusAndDeletedFalse(direction, status, pageable);
        } else {
            orders = auctionOrderRepository.findByDeletedFalse(pageable);
        }
        return orders.map(this::toOrderResponse);
    }

    /**
     * 查询我的挂单
     */
    public Page<AuctionOrderResponse> listMyOrders(JwtUserDetails currentUser,
                                                     Integer direction, Integer status,
                                                     Integer page, Integer size) {
        size = CommonUtils.sanitizePageSize(size);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuctionOrder> orders;
        if (direction != null && status != null) {
            orders = auctionOrderRepository.findByUserIdAndDirectionAndStatusAndDeletedFalse(
                    currentUser.getUserId(), direction, status, pageable);
        } else if (direction != null) {
            orders = auctionOrderRepository.findByUserIdAndDirectionAndDeletedFalse(
                    currentUser.getUserId(), direction, pageable);
        } else {
            orders = auctionOrderRepository.findByUserIdAndDeletedFalse(currentUser.getUserId(), pageable);
        }
        return orders.map(this::toOrderResponse);
    }

    /**
     * 查询撮合结果
     */
    public Page<MatchingResultResponse> listMatchingResults(JwtUserDetails currentUser,
                                                              Integer page, Integer size) {
        size = CommonUtils.sanitizePageSize(size);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MatchingResult> results = matchingResultRepository.findByUserIdRelated(currentUser.getUserId(), pageable);
        Map<Long, String> userNames = resolveUserNames(results.getContent());
        return results.map(m -> toMatchResponse(m, userNames));
    }

    // ==================== 私有方法 ====================

    /**
     * 更新挂单状态（根据匹配数量判断）
     */
    private void updateOrderStatus(AuctionOrder order) {
        BigDecimal remaining = order.getQuantity().subtract(order.getMatchedQuantity());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            order.setStatus(AuctionOrderStatusEnum.FULLY_MATCHED.getCode());
            order.setMatchedAt(LocalDateTime.now());
        } else {
            order.setStatus(AuctionOrderStatusEnum.PARTIALLY_MATCHED.getCode());
        }
    }

    /**
     * 更新买卖双方企业配额
     */
    private void updateEnterpriseQuota(Long buyerUserId, Long sellerUserId, BigDecimal quantity) {
        Enterprise seller = enterpriseRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> TradeException.insufficientQuota(BigDecimal.ZERO, quantity));
        Enterprise buyer = enterpriseRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> TradeException.insufficientQuota(BigDecimal.ZERO, quantity));

        seller.setCarbonTradable(seller.getCarbonTradable().subtract(quantity));
        seller.setCarbonUsed(seller.getCarbonUsed().add(quantity));

        buyer.setCarbonQuota(buyer.getCarbonQuota().add(quantity));
        buyer.setCarbonTradable(buyer.getCarbonTradable().add(quantity));

        enterpriseRepository.save(seller);
        enterpriseRepository.save(buyer);
    }

    /**
     * 生成挂单编号
     */
    private String generateOrderNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", MATCH_RNG.nextInt(10000));
    }

    /**
     * 生成撮合编号
     */
    private String generateMatchNo() {
        return "MT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", MATCH_RNG.nextInt(10000));
    }

    /**
     * AuctionOrder → AuctionOrderResponse
     */
    private AuctionOrderResponse toOrderResponse(AuctionOrder order) {
        BigDecimal remaining = order.getQuantity().subtract(
                order.getMatchedQuantity() != null ? order.getMatchedQuantity() : BigDecimal.ZERO);

        String directionText = order.getDirection() == 1 ? "买入" : "卖出";
        String statusText = AuctionOrderStatusEnum.fromCode(order.getStatus()) != null
                ? AuctionOrderStatusEnum.fromCode(order.getStatus()).getDescription() : "";

        return AuctionOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .direction(order.getDirection())
                .directionText(directionText)
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .matchedQuantity(order.getMatchedQuantity())
                .remainingQuantity(remaining)
                .status(order.getStatus())
                .statusText(statusText)
                .settlementPrice(order.getSettlementPrice())
                .matchedAt(order.getMatchedAt())
                .createdAt(order.getCreatedAt())
                .build();
    }

    /**
     * 批量解析撮合结果涉及的用户名称（解决 N+1 查询问题）
     */
    private Map<Long, String> resolveUserNames(List<MatchingResult> matches) {
        Set<Long> userIds = new HashSet<>();
        for (MatchingResult m : matches) {
            userIds.add(m.getBuyerId());
            userIds.add(m.getSellerId());
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));
    }

    /**
     * MatchingResult → MatchingResultResponse
     */
    private MatchingResultResponse toMatchResponse(MatchingResult match) {
        String buyerName = userRepository.findById(match.getBuyerId())
                .map(User::getRealName).orElse("未知");
        String sellerName = userRepository.findById(match.getSellerId())
                .map(User::getRealName).orElse("未知");
        String statusText = MatchingStatusEnum.fromCode(match.getStatus()) != null
                ? MatchingStatusEnum.fromCode(match.getStatus()).getDescription() : "";

        return buildMatchResponse(match, buyerName, sellerName, statusText);
    }

    private MatchingResultResponse toMatchResponse(MatchingResult match, Map<Long, String> userNames) {
        String buyerName = userNames.getOrDefault(match.getBuyerId(), "未知");
        String sellerName = userNames.getOrDefault(match.getSellerId(), "未知");
        String statusText = MatchingStatusEnum.fromCode(match.getStatus()) != null
                ? MatchingStatusEnum.fromCode(match.getStatus()).getDescription() : "";

        return buildMatchResponse(match, buyerName, sellerName, statusText);
    }

    private MatchingResultResponse buildMatchResponse(MatchingResult match, String buyerName,
                                                       String sellerName, String statusText) {

        return MatchingResultResponse.builder()
                .id(match.getId())
                .matchNo(match.getMatchNo())
                .buyOrderId(match.getBuyOrderId())
                .sellOrderId(match.getSellOrderId())
                .buyerId(match.getBuyerId())
                .sellerId(match.getSellerId())
                .buyerName(buyerName)
                .sellerName(sellerName)
                .matchedQuantity(match.getMatchedQuantity())
                .settlementPrice(match.getSettlementPrice())
                .totalAmount(match.getTotalAmount())
                .status(match.getStatus())
                .statusText(statusText)
                .transactionId(match.getTransactionId())
                .settledAt(match.getSettledAt())
                .createdAt(match.getCreatedAt())
                .build();
    }
}
