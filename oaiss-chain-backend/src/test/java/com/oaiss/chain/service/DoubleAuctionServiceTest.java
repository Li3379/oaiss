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
import com.oaiss.chain.exception.TradeException;
import com.oaiss.chain.repository.AuctionOrderRepository;
import com.oaiss.chain.repository.EnterpriseRepository;
import com.oaiss.chain.repository.MatchingResultRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * DoubleAuctionService 单元测试
 * DoubleAuctionService Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class DoubleAuctionServiceTest {

    @Mock
    private AuctionOrderRepository auctionOrderRepository;

    @Mock
    private MatchingResultRepository matchingResultRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DoubleAuctionService doubleAuctionService;

    private JwtUserDetails currentUser;
    private Enterprise testEnterprise;
    private AuctionOrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        currentUser = new JwtUserDetails();
        currentUser.setUserId(1L);
        currentUser.setUsername("testuser");

        testEnterprise = Enterprise.builder()
                .userId(1L)
                .carbonQuota(new BigDecimal("1000.00"))
                .carbonTradable(new BigDecimal("500.00"))
                .carbonUsed(new BigDecimal("500.00"))
                .build();
        testEnterprise.setId(1L);

        orderRequest = new AuctionOrderRequest();
        orderRequest.setQuantity(new BigDecimal("100.00"));
        orderRequest.setPrice(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("提交买入挂单成功")
    void testPlaceBuyOrderSuccess() {
        // Given
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(auctionOrderRepository.save(any(AuctionOrder.class))).thenAnswer(invocation -> {
            AuctionOrder order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        AuctionOrderResponse response = doubleAuctionService.placeBuyOrder(currentUser, orderRequest);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getDirection());
        assertEquals(new BigDecimal("100.00"), response.getQuantity());
        assertEquals(new BigDecimal("50.00"), response.getPrice());
        verify(auctionOrderRepository, times(1)).save(any(AuctionOrder.class));
    }

    @Test
    @DisplayName("提交买入挂单失败-企业不存在")
    void testPlaceBuyOrderFailEnterpriseNotFound() {
        // Given
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(TradeException.class, () -> 
            doubleAuctionService.placeBuyOrder(currentUser, orderRequest));
        verify(auctionOrderRepository, never()).save(any());
    }

    // ========== TDD Cycle 2: H17 买入挂单未验证配额 ==========

    @Test
    @DisplayName("买入挂单失败-请求量超过企业可交易配额")
    void testPlaceBuyOrderFailInsufficientQuota() {
        // 企业可交易配额只有 500，但买入请求 1000
        orderRequest.setQuantity(new BigDecimal("1000.00"));

        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));

        // RED: 当前实现不检查买入方配额（BUG），此测试应失败
        assertThrows(TradeException.class,
                () -> doubleAuctionService.placeBuyOrder(currentUser, orderRequest));
        verify(auctionOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("提交卖出挂单成功")
    void testPlaceSellOrderSuccess() {
        // Given
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(auctionOrderRepository.save(any(AuctionOrder.class))).thenAnswer(invocation -> {
            AuctionOrder order = invocation.getArgument(0);
            order.setId(2L);
            return order;
        });

        // When
        AuctionOrderResponse response = doubleAuctionService.placeSellOrder(currentUser, orderRequest);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getDirection());
        verify(auctionOrderRepository, times(1)).save(any(AuctionOrder.class));
    }

    @Test
    @DisplayName("提交卖出挂单失败-可交易配额不足")
    void testPlaceSellOrderFailInsufficientQuota() {
        // Given
        orderRequest.setQuantity(new BigDecimal("1000.00")); // More than tradable (500)
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));

        // When & Then
        assertThrows(TradeException.class, () -> 
            doubleAuctionService.placeSellOrder(currentUser, orderRequest));
        verify(auctionOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("执行撮合-无匹配机会")
    void testExecuteMatchingNoOpportunity() {
        // Given
        when(auctionOrderRepository.findByDirectionAndStatusInAndDeletedFalseOrderByPriceDesc(eq(1), anyList()))
                .thenReturn(Collections.emptyList());
        when(auctionOrderRepository.findByDirectionAndStatusInAndDeletedFalseOrderByPriceAsc(eq(2), anyList()))
                .thenReturn(Collections.emptyList());

        // When
        var results = doubleAuctionService.executeMatching();

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("执行撮合-成功匹配")
    void testExecuteMatchingSuccess() {
        // Given
        AuctionOrder buyOrder = AuctionOrder.builder()
                .orderNo("B20240101001")
                .userId(1L)
                .direction(1)
                .quantity(new BigDecimal("100.00"))
                .price(new BigDecimal("60.00"))
                .matchedQuantity(BigDecimal.ZERO)
                .status(AuctionOrderStatusEnum.PENDING.getCode())
                .build();
        buyOrder.setId(1L);

        AuctionOrder sellOrder = AuctionOrder.builder()
                .orderNo("S20240101001")
                .userId(2L)
                .direction(2)
                .quantity(new BigDecimal("100.00"))
                .price(new BigDecimal("50.00"))
                .matchedQuantity(BigDecimal.ZERO)
                .status(AuctionOrderStatusEnum.PENDING.getCode())
                .build();
        sellOrder.setId(2L);

        Enterprise buyerEnterprise = Enterprise.builder()
                .userId(1L)
                .carbonQuota(new BigDecimal("1000.00"))
                .carbonTradable(new BigDecimal("500.00"))
                .carbonUsed(new BigDecimal("500.00"))
                .build();
        buyerEnterprise.setId(1L);

        Enterprise sellerEnterprise = Enterprise.builder()
                .userId(2L)
                .carbonQuota(new BigDecimal("1000.00"))
                .carbonTradable(new BigDecimal("500.00"))
                .carbonUsed(new BigDecimal("500.00"))
                .build();
        sellerEnterprise.setId(2L);

        User buyer = User.builder().realName("Buyer").build();
        buyer.setId(1L);
        User seller = User.builder().realName("Seller").build();
        seller.setId(2L);

        when(auctionOrderRepository.findByDirectionAndStatusInAndDeletedFalseOrderByPriceDesc(eq(1), anyList()))
                .thenReturn(Arrays.asList(buyOrder));
        when(auctionOrderRepository.findByDirectionAndStatusInAndDeletedFalseOrderByPriceAsc(eq(2), anyList()))
                .thenReturn(Arrays.asList(sellOrder));
        when(matchingResultRepository.save(any(MatchingResult.class))).thenAnswer(invocation -> {
            MatchingResult match = invocation.getArgument(0);
            match.setId(1L);
            return match;
        });
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(1L);
            return tx;
        });
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(buyerEnterprise));
        when(enterpriseRepository.findByUserId(2L)).thenReturn(Optional.of(sellerEnterprise));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(auctionOrderRepository.save(any(AuctionOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var results = doubleAuctionService.executeMatching();

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(matchingResultRepository, atLeastOnce()).save(any(MatchingResult.class));
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("分页查询挂单")
    void testListOrders() {
        // Given
        AuctionOrder order = AuctionOrder.builder()
                .orderNo("B20240101001")
                .userId(1L)
                .direction(1)
                .quantity(new BigDecimal("100.00"))
                .price(new BigDecimal("50.00"))
                .matchedQuantity(BigDecimal.ZERO)
                .status(AuctionOrderStatusEnum.PENDING.getCode())
                .build();
        order.setId(1L);

        Page<AuctionOrder> page = new PageImpl<>(Arrays.asList(order));
        when(auctionOrderRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(page);

        // When
        Page<AuctionOrderResponse> result = doubleAuctionService.listOrders(null, null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("查询我的挂单")
    void testListMyOrders() {
        // Given
        AuctionOrder order = AuctionOrder.builder()
                .orderNo("B20240101001")
                .userId(1L)
                .direction(1)
                .quantity(new BigDecimal("100.00"))
                .price(new BigDecimal("50.00"))
                .matchedQuantity(BigDecimal.ZERO)
                .status(AuctionOrderStatusEnum.PENDING.getCode())
                .build();
        order.setId(1L);

        Page<AuctionOrder> page = new PageImpl<>(Arrays.asList(order));
        when(auctionOrderRepository.findByUserIdAndDeletedFalse(eq(1L), any(Pageable.class))).thenReturn(page);

        // When
        Page<AuctionOrderResponse> result = doubleAuctionService.listMyOrders(currentUser, null, null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("查询撮合结果")
    void testListMatchingResults() {
        // Given
        MatchingResult match = MatchingResult.builder()
                .matchNo("MT20240101001")
                .buyOrderId(1L)
                .sellOrderId(2L)
                .buyerId(1L)
                .sellerId(2L)
                .matchedQuantity(new BigDecimal("100.00"))
                .settlementPrice(new BigDecimal("55.00"))
                .totalAmount(new BigDecimal("5500.00"))
                .build();
        match.setId(1L);

        User buyer = User.builder().realName("Buyer").build();
        buyer.setId(1L);
        User seller = User.builder().realName("Seller").build();
        seller.setId(2L);

        Page<MatchingResult> page = new PageImpl<>(Arrays.asList(match));
        when(matchingResultRepository.findByUserIdRelated(eq(1L), any(Pageable.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(Arrays.asList(buyer, seller));

        // When
        Page<MatchingResultResponse> result = doubleAuctionService.listMatchingResults(currentUser, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== C6: Matching Engine Concurrent Lock ====================

    @Test
    @DisplayName("executeMatching方法应使用@DistributedLock防止并发撮合")
    void testExecuteMatchingHasDistributedLock() throws Exception {
        // Verify the method has @DistributedLock annotation instead of synchronized
        var method = DoubleAuctionService.class.getMethod("executeMatching");
        var annotation = method.getAnnotation(com.oaiss.chain.annotation.DistributedLock.class);
        assertNotNull(annotation,
                "executeMatching() must have @DistributedLock annotation for distributed concurrency control");
        assertEquals("'auction:matching'", annotation.key(),
                "@DistributedLock key must be 'auction:matching'");
        assertFalse(java.lang.reflect.Modifier.isSynchronized(method.getModifiers()),
                "executeMatching() must NOT be synchronized — use @DistributedLock instead");
    }
}
