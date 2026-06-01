package com.oaiss.chain.service;

import com.oaiss.chain.constant.ErrorCode;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        orderRequest.setDirection(1);
        orderRequest.setQuantity(new BigDecimal("100.00"));
        orderRequest.setPrice(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("placeBuyOrder succeeds")
    void testPlaceBuyOrderSuccess() {
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(auctionOrderRepository.save(any(AuctionOrder.class))).thenAnswer(invocation -> {
            AuctionOrder order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        AuctionOrderResponse response = doubleAuctionService.placeBuyOrder(currentUser, orderRequest);

        assertNotNull(response);
        assertEquals(1, response.getDirection());
        assertEquals(new BigDecimal("100.00"), response.getQuantity());
        assertEquals(new BigDecimal("50.00"), response.getPrice());
        verify(auctionOrderRepository, times(1)).save(any(AuctionOrder.class));
    }

    @Test
    @DisplayName("placeBuyOrder fails when enterprise is missing")
    void testPlaceBuyOrderFailEnterpriseNotFound() {
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(TradeException.class, () ->
                doubleAuctionService.placeBuyOrder(currentUser, orderRequest));
        verify(auctionOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("placeBuyOrder fails when quantity exceeds tradable quota")
    void testPlaceBuyOrderFailInsufficientQuota() {
        orderRequest.setQuantity(new BigDecimal("1000.00"));
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));

        assertThrows(TradeException.class, () ->
                doubleAuctionService.placeBuyOrder(currentUser, orderRequest));
        verify(auctionOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("placeBuyOrder rejects negative price before repository access")
    void testPlaceBuyOrderFailNegativePrice() {
        orderRequest.setPrice(new BigDecimal("-1.00"));

        TradeException ex = assertThrows(TradeException.class, () ->
                doubleAuctionService.placeBuyOrder(currentUser, orderRequest));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
        verify(enterpriseRepository, never()).findByUserId(any());
        verify(auctionOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("placeSellOrder succeeds")
    void testPlaceSellOrderSuccess() {
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(auctionOrderRepository.save(any(AuctionOrder.class))).thenAnswer(invocation -> {
            AuctionOrder order = invocation.getArgument(0);
            order.setId(2L);
            return order;
        });

        AuctionOrderResponse response = doubleAuctionService.placeSellOrder(currentUser, orderRequest);

        assertNotNull(response);
        assertEquals(2, response.getDirection());
        verify(auctionOrderRepository, times(1)).save(any(AuctionOrder.class));
    }

    @Test
    @DisplayName("placeSellOrder fails when quantity exceeds tradable quota")
    void testPlaceSellOrderFailInsufficientQuota() {
        orderRequest.setQuantity(new BigDecimal("1000.00"));
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));

        assertThrows(TradeException.class, () ->
                doubleAuctionService.placeSellOrder(currentUser, orderRequest));
        verify(auctionOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("placeSellOrder rejects zero quantity before repository access")
    void testPlaceSellOrderFailZeroQuantity() {
        orderRequest.setQuantity(BigDecimal.ZERO);

        TradeException ex = assertThrows(TradeException.class, () ->
                doubleAuctionService.placeSellOrder(currentUser, orderRequest));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
        verify(enterpriseRepository, never()).findByUserId(any());
        verify(auctionOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("executeMatching returns empty list when there is no opportunity")
    void testExecuteMatchingNoOpportunity() {
        when(auctionOrderRepository.findByDirectionAndStatusInAndDeletedFalseOrderByPriceDesc(eq(1), anyList()))
                .thenReturn(Collections.emptyList());
        when(auctionOrderRepository.findByDirectionAndStatusInAndDeletedFalseOrderByPriceAsc(eq(2), anyList()))
                .thenReturn(Collections.emptyList());

        var results = doubleAuctionService.executeMatching();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("executeMatching settles one match")
    void testExecuteMatchingSuccess() {
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

        var results = doubleAuctionService.executeMatching();

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(matchingResultRepository, atLeastOnce()).save(any(MatchingResult.class));
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("listOrders returns paged responses")
    void testListOrders() {
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

        Page<AuctionOrderResponse> result = doubleAuctionService.listOrders(null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("listMyOrders returns current user orders")
    void testListMyOrders() {
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

        Page<AuctionOrderResponse> result = doubleAuctionService.listMyOrders(currentUser, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("listMatchingResults returns user-related settlements")
    void testListMatchingResults() {
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

        Page<MatchingResultResponse> result = doubleAuctionService.listMatchingResults(currentUser, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("executeMatching uses DistributedLock instead of synchronized")
    void testExecuteMatchingHasDistributedLock() throws Exception {
        var method = DoubleAuctionService.class.getMethod("executeMatching");
        var annotation = method.getAnnotation(com.oaiss.chain.annotation.DistributedLock.class);

        assertNotNull(annotation,
                "executeMatching() must have @DistributedLock annotation for distributed concurrency control");
        assertEquals("'auction:matching'", annotation.key(),
                "@DistributedLock key must be 'auction:matching'");
        assertFalse(java.lang.reflect.Modifier.isSynchronized(method.getModifiers()),
                "executeMatching() must NOT be synchronized; use @DistributedLock instead");
    }
}
