package com.oaiss.chain.service;

import com.oaiss.chain.dto.TradeRequest;
import com.oaiss.chain.dto.TradeResponse;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.entity.Transaction;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.enums.TradeStatusEnum;
import com.oaiss.chain.enums.TradeTypeEnum;
import com.oaiss.chain.exception.TradeException;
import com.oaiss.chain.repository.EnterpriseRepository;
import com.oaiss.chain.repository.TransactionRepository;
import com.oaiss.chain.repository.UserRepository;
import com.oaiss.chain.security.JwtUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TradeService 单元测试
 * TradeService Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private TradeService tradeService;

    private Transaction testTransaction;
    private Enterprise sellerEnterprise;
    private Enterprise buyerEnterprise;
    private JwtUserDetails currentUser;
    private TradeRequest tradeRequest;

    @BeforeEach
    void setUp() {
        testTransaction = Transaction.builder()
                .tradeNo("TX20240101001")
                .tradeType(TradeTypeEnum.P2P.getCode())
                .sellerId(1L)
                .buyerId(2L)
                .quantity(new BigDecimal("100.00"))
                .unitPrice(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("5000.00"))
                .status(TradeStatusEnum.PENDING.getCode())
                .build();
        testTransaction.setId(1L);

        sellerEnterprise = Enterprise.builder()
                .userId(1L)
                .carbonQuota(new BigDecimal("1000.00"))
                .carbonTradable(new BigDecimal("500.00"))
                .carbonUsed(new BigDecimal("500.00"))
                .build();
        sellerEnterprise.setId(1L);

        buyerEnterprise = Enterprise.builder()
                .userId(2L)
                .carbonQuota(new BigDecimal("500.00"))
                .carbonTradable(new BigDecimal("300.00"))
                .carbonUsed(new BigDecimal("200.00"))
                .build();
        buyerEnterprise.setId(2L);

        currentUser = new JwtUserDetails();
        currentUser.setUserId(1L);
        currentUser.setUsername("testuser");

        tradeRequest = new TradeRequest();
        tradeRequest.setTradeType(TradeTypeEnum.P2P.getCode());
        tradeRequest.setSellerId(1L);
        tradeRequest.setBuyerId(2L);
        tradeRequest.setQuantity(new BigDecimal("100.00"));
        tradeRequest.setUnitPrice(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("创建P2P交易成功")
    void testCreateP2PTradeSuccess() {
        // Given
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(sellerEnterprise));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        TradeResponse response = tradeService.createP2PTrade(currentUser, tradeRequest);

        // Then
        assertNotNull(response);
        assertEquals(TradeTypeEnum.P2P.getCode(), response.getTradeType());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("创建P2P交易失败-双方相同")
    void testCreateP2PTradeFailSameParty() {
        // Given
        tradeRequest.setSellerId(1L);
        tradeRequest.setBuyerId(1L);

        // When & Then
        assertThrows(TradeException.class, () -> tradeService.createP2PTrade(currentUser, tradeRequest));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("创建P2P交易失败-配额不足")
    void testCreateP2PTradeFailInsufficientQuota() {
        // Given
        tradeRequest.setQuantity(new BigDecimal("1000.00")); // More than tradable (500)
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(sellerEnterprise));

        // When & Then
        assertThrows(TradeException.class, () -> tradeService.createP2PTrade(currentUser, tradeRequest));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("创建拍卖挂单成功")
    void testCreateAuctionOrderSuccess() {
        // Given
        tradeRequest.setTradeType(TradeTypeEnum.AUCTION.getCode());
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(sellerEnterprise));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        TradeResponse response = tradeService.createAuctionOrder(currentUser, tradeRequest);

        // Then
        assertNotNull(response);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("确认交易成功")
    void testConfirmTradeSuccess() {
        // Given
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(sellerEnterprise));
        when(enterpriseRepository.findByUserId(2L)).thenReturn(Optional.of(buyerEnterprise));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(enterpriseRepository.save(any(Enterprise.class))).thenReturn(sellerEnterprise);

        // When
        TradeResponse response = tradeService.confirmTrade(1L, 1L);

        // Then
        assertNotNull(response);
        verify(enterpriseRepository, times(2)).save(any(Enterprise.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // PROCESSING + COMPLETED
    }

    @Test
    @DisplayName("确认交易失败-交易不存在")
    void testConfirmTradeFailNotFound() {
        // Given
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(TradeException.class, () -> tradeService.confirmTrade(999L, 1L));
    }

    @Test
    @DisplayName("取消交易成功")
    void testCancelTradeSuccess() {
        // Given
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        TradeResponse response = tradeService.cancelTrade(1L, currentUser);

        // Then
        assertNotNull(response);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("获取交易详情成功")
    void testGetTradeSuccess() {
        // Given
        User seller = User.builder().username("seller").realName("Seller").build();
        seller.setId(1L);
        User buyer = User.builder().username("buyer").realName("Buyer").build();
        buyer.setId(2L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));

        // When
        TradeResponse response = tradeService.getTrade(1L, currentUser);

        // Then
        assertNotNull(response);
        assertEquals("TX20240101001", response.getTradeNo());
    }

    @Test
    @DisplayName("获取交易详情失败-不存在")
    void testGetTradeFailNotFound() {
        // Given
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(TradeException.class, () -> tradeService.getTrade(999L, currentUser));
    }

    // ========== TDD Cycle 1: C4 P2P交易越权漏洞 ==========

    @Test
    @DisplayName("P2P交易失败-当前用户不是卖方(越权)")
    void testCreateP2PTradeFailSellerNotCurrentUser() {
        // 当前用户 ID=1，但请求中 sellerId=999（冒充他人卖方）
        tradeRequest.setSellerId(999L);
        tradeRequest.setBuyerId(2L);

        assertThrows(TradeException.class,
                () -> tradeService.createP2PTrade(currentUser, tradeRequest));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("分页查询交易")
    void testListTrades() {
        // Given
        User seller = User.builder().username("seller").realName("Seller").build();
        seller.setId(1L);
        User buyer = User.builder().username("buyer").realName("Buyer").build();
        buyer.setId(2L);

        Page<Transaction> page = new PageImpl<>(Arrays.asList(testTransaction));
        when(transactionRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(Arrays.asList(seller, buyer));

        // When
        Page<TradeResponse> result = tradeService.listTrades(null, null, null, null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== H6: Trade Confirmation Race Condition ====================

    @Test
    @DisplayName("确认交易失败-交易已完成（防止重复确认竞态）")
    void testConfirmTradeFailAlreadyCompleted() {
        // Given: trade is already COMPLETED
        testTransaction.setStatus(TradeStatusEnum.COMPLETED.getCode());
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // When & Then: should reject immediately, no quota changes
        assertThrows(TradeException.class, () -> tradeService.confirmTrade(1L, 1L));
        verify(enterpriseRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("确认交易失败-交易处理中（防止并发确认）")
    void testConfirmTradeFailProcessing() {
        // Given: trade is already PROCESSING (another request started confirmation)
        testTransaction.setStatus(TradeStatusEnum.PROCESSING.getCode());
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // When & Then: should reject due to PROCESSING status
        assertThrows(TradeException.class, () -> tradeService.confirmTrade(1L, 1L));
        // Quotas should NOT be modified
        verify(enterpriseRepository, never()).save(any());
    }

    @Test
    @DisplayName("确认交易失败-交易已取消")
    void testConfirmTradeFailCancelled() {
        // Given: trade is CANCELLED
        testTransaction.setStatus(TradeStatusEnum.CANCELLED.getCode());
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // When & Then
        assertThrows(TradeException.class, () -> tradeService.confirmTrade(1L, 1L));
        verify(enterpriseRepository, never()).save(any());
    }

    @Test
    @DisplayName("确认交易成功-待处理状态应允许确认")
    void testConfirmTradeSuccessPendingStatus() {
        // Given: trade is PENDING (valid for confirmation)
        testTransaction.setStatus(TradeStatusEnum.PENDING.getCode());
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(sellerEnterprise));
        when(enterpriseRepository.findByUserId(2L)).thenReturn(Optional.of(buyerEnterprise));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(enterpriseRepository.save(any(Enterprise.class))).thenReturn(sellerEnterprise);

        // When & Then: should succeed
        TradeResponse response = tradeService.confirmTrade(1L, 1L);
        assertNotNull(response);
    }
}
