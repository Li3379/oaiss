package com.oaiss.chain.service;

import com.oaiss.chain.dto.CarbonCoinAccountResponse;
import com.oaiss.chain.dto.CarbonCoinRechargeRequest;
import com.oaiss.chain.dto.CarbonCoinTransferRequest;
import com.oaiss.chain.entity.CarbonCoinAccount;
import com.oaiss.chain.entity.CarbonCoinTransaction;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.CarbonCoinAccountRepository;
import com.oaiss.chain.repository.CarbonCoinTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CarbonCoinService 单元测试
 * CarbonCoinService Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class CarbonCoinServiceTest {

    @Mock
    private CarbonCoinAccountRepository accountRepository;

    @Mock
    private CarbonCoinTransactionRepository transactionRepository;

    @InjectMocks
    private CarbonCoinService carbonCoinService;

    private CarbonCoinAccount testAccount;
    private CarbonCoinAccount counterpartAccount;

    @BeforeEach
    void setUp() {
        testAccount = CarbonCoinAccount.builder()
                .userId(1L)
                .balance(new BigDecimal("1000.00"))
                .totalRecharged(new BigDecimal("2000.00"))
                .totalSpent(new BigDecimal("1000.00"))
                .status(1)
                .build();
        testAccount.setId(1L);

        counterpartAccount = CarbonCoinAccount.builder()
                .userId(2L)
                .balance(new BigDecimal("500.00"))
                .totalRecharged(new BigDecimal("500.00"))
                .totalSpent(BigDecimal.ZERO)
                .status(1)
                .build();
        counterpartAccount.setId(2L);
    }

    @Test
    @DisplayName("获取或创建账户-已存在")
    void testGetOrCreateAccountExisting() {
        // Given
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When
        CarbonCoinAccountResponse response = carbonCoinService.getOrCreateAccount(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(new BigDecimal("1000.00"), response.getBalance());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("获取或创建账户-新建")
    void testGetOrCreateAccountNew() {
        // Given
        CarbonCoinAccount newAccount = CarbonCoinAccount.builder()
                .userId(3L)
                .balance(BigDecimal.ZERO)
                .totalRecharged(BigDecimal.ZERO)
                .totalSpent(BigDecimal.ZERO)
                .status(1)
                .build();
        newAccount.setId(3L);

        when(accountRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(accountRepository.save(any(CarbonCoinAccount.class))).thenReturn(newAccount);

        // When
        CarbonCoinAccountResponse response = carbonCoinService.getOrCreateAccount(3L);

        // Then
        assertNotNull(response);
        assertEquals(3L, response.getUserId());
        verify(accountRepository, times(1)).save(any(CarbonCoinAccount.class));
    }

    @Test
    @DisplayName("充值碳币成功")
    void testRechargeSuccess() {
        // Given
        CarbonCoinRechargeRequest request = new CarbonCoinRechargeRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setRemark("测试充值");

        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(CarbonCoinAccount.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(CarbonCoinTransaction.class))).thenReturn(new CarbonCoinTransaction());

        // When
        CarbonCoinAccountResponse response = carbonCoinService.recharge(1L, request);

        // Then
        assertNotNull(response);
        verify(accountRepository, times(1)).save(any(CarbonCoinAccount.class));
        verify(transactionRepository, times(1)).save(any(CarbonCoinTransaction.class));
    }

    @Test
    @DisplayName("充值碳币失败-账户已禁用")
    void testRechargeFailAccountDisabled() {
        // Given
        testAccount.setStatus(0);
        CarbonCoinRechargeRequest request = new CarbonCoinRechargeRequest();
        request.setAmount(new BigDecimal("500.00"));

        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThrows(BusinessException.class, () -> carbonCoinService.recharge(1L, request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("购买碳配额成功")
    void testBuyQuotaSuccess() {
        // Given
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(CarbonCoinAccount.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(CarbonCoinTransaction.class))).thenReturn(new CarbonCoinTransaction());

        // When
        CarbonCoinAccountResponse response = carbonCoinService.buyQuota(
                1L, new BigDecimal("100.00"), new BigDecimal("10.00"), 1L);

        // Then
        assertNotNull(response);
        verify(accountRepository, times(1)).save(any(CarbonCoinAccount.class));
        verify(transactionRepository, times(1)).save(any(CarbonCoinTransaction.class));
    }

    @Test
    @DisplayName("购买碳配额失败-余额不足")
    void testBuyQuotaFailInsufficientBalance() {
        // Given
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When & Then - trying to buy more than balance (1000)
        assertThrows(BusinessException.class, () -> 
            carbonCoinService.buyQuota(1L, new BigDecimal("2000.00"), new BigDecimal("100.00"), 1L));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("出售碳配额成功")
    void testSellQuotaSuccess() {
        // Given
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(CarbonCoinAccount.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(CarbonCoinTransaction.class))).thenReturn(new CarbonCoinTransaction());

        // When
        CarbonCoinAccountResponse response = carbonCoinService.sellQuota(
                1L, new BigDecimal("100.00"), new BigDecimal("10.00"), 1L);

        // Then
        assertNotNull(response);
        verify(accountRepository, times(1)).save(any(CarbonCoinAccount.class));
        verify(transactionRepository, times(1)).save(any(CarbonCoinTransaction.class));
    }

    @Test
    @DisplayName("碳币转账成功")
    void testTransferSuccess() {
        // Given
        CarbonCoinTransferRequest request = new CarbonCoinTransferRequest();
        request.setCounterpartId(2L);
        request.setAmount(new BigDecimal("100.00"));
        request.setRemark("测试转账");

        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findByUserId(2L)).thenReturn(Optional.of(counterpartAccount));
        when(accountRepository.save(any(CarbonCoinAccount.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(CarbonCoinTransaction.class))).thenReturn(new CarbonCoinTransaction());

        // When
        CarbonCoinAccountResponse response = carbonCoinService.transfer(1L, request);

        // Then
        assertNotNull(response);
        verify(accountRepository, times(2)).save(any(CarbonCoinAccount.class));
        verify(transactionRepository, times(2)).save(any(CarbonCoinTransaction.class));
    }

    @Test
    @DisplayName("碳币转账失败-不能向自己转账")
    void testTransferFailSelfTransfer() {
        // Given
        CarbonCoinTransferRequest request = new CarbonCoinTransferRequest();
        request.setCounterpartId(1L);
        request.setAmount(new BigDecimal("100.00"));

        // When & Then
        assertThrows(BusinessException.class, () -> carbonCoinService.transfer(1L, request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("碳币转账失败-余额不足")
    void testTransferFailInsufficientBalance() {
        // Given
        CarbonCoinTransferRequest request = new CarbonCoinTransferRequest();
        request.setCounterpartId(2L);
        request.setAmount(new BigDecimal("2000.00")); // More than balance (1000)

        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThrows(BusinessException.class, () -> carbonCoinService.transfer(1L, request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("碳币转账失败-对方账户不存在")
    void testTransferFailCounterpartNotFound() {
        // Given
        CarbonCoinTransferRequest request = new CarbonCoinTransferRequest();
        request.setCounterpartId(999L);
        request.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BusinessException.class, () -> carbonCoinService.transfer(1L, request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("查询交易流水")
    void testGetTransactions() {
        // Given
        CarbonCoinTransaction tx1 = CarbonCoinTransaction.builder()
                .userId(1L)
                .txType(1)
                .amount(new BigDecimal("100.00"))
                .build();
        tx1.setId(1L);
        CarbonCoinTransaction tx2 = CarbonCoinTransaction.builder()
                .userId(1L)
                .txType(2)
                .amount(new BigDecimal("50.00"))
                .build();
        tx2.setId(2L);

        Page<CarbonCoinTransaction> page = new PageImpl<>(Arrays.asList(tx1, tx2));
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(page);

        // When
        Page<CarbonCoinTransaction> result = carbonCoinService.getTransactions(1L, null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    @DisplayName("查询交易流水-按类型筛选")
    void testGetTransactionsByType() {
        // Given
        CarbonCoinTransaction tx = CarbonCoinTransaction.builder()
                .userId(1L)
                .txType(1)
                .amount(new BigDecimal("100.00"))
                .build();
        tx.setId(1L);

        Page<CarbonCoinTransaction> page = new PageImpl<>(Arrays.asList(tx));
        when(transactionRepository.findByUserIdAndTxTypeOrderByCreatedAtDesc(eq(1L), eq(1), any(PageRequest.class)))
                .thenReturn(page);

        // When
        Page<CarbonCoinTransaction> result = carbonCoinService.getTransactions(1L, 1, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getContent().get(0).getTxType());
    }
}
