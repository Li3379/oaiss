package com.oaiss.chain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.dto.CarbonCoinAccountResponse;
import com.oaiss.chain.dto.CarbonCoinRechargeRequest;
import com.oaiss.chain.dto.CarbonCoinTransferRequest;
import com.oaiss.chain.entity.CarbonCoinTransaction;
import com.oaiss.chain.security.JwtTokenProvider;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.CarbonCoinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CarbonCoinController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
        })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CarbonCoinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CarbonCoinService carbonCoinService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private CarbonCoinAccountResponse accountResponse;
    private CarbonCoinTransaction transaction;
    private JwtUserDetails currentUser;
    private JwtUserDetails adminUser;

    @BeforeEach
    void setUp() {
        accountResponse = CarbonCoinAccountResponse.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("10000.00"))
                .totalRecharged(new BigDecimal("10000.00"))
                .totalSpent(BigDecimal.ZERO)
                .status(1)
                .build();

        transaction = new CarbonCoinTransaction();
        transaction.setId(1L);
        transaction.setTxNo("TXN202401010001");
        transaction.setUserId(1L);
        transaction.setTxType(1);
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setBalanceBefore(new BigDecimal("9000.00"));
        transaction.setBalanceAfter(new BigDecimal("10000.00"));

        SecurityContextHolder.clearContext();

        // Create test users
        currentUser = JwtUserDetails.builder()
                .userId(1L)
                .username("user@example.com")
                .password("password")
                .userType(1)
                .roles(List.of("ENTERPRISE"))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        adminUser = JwtUserDetails.builder()
                .userId(1L)
                .username("admin@example.com")
                .password("password")
                .userType(1)
                .roles(List.of("ADMIN"))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    private void setAuthentication(JwtUserDetails user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("获取碳币账户成功")
    void testGetAccountSuccess() throws Exception {
        setAuthentication(currentUser);
        when(carbonCoinService.getOrCreateAccount(1L)).thenReturn(accountResponse);

        mockMvc.perform(get("/carbon-coin/account")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.balance").value(10000.00));

        verify(carbonCoinService).getOrCreateAccount(1L);
    }

    @Test
    @DisplayName("充值碳币成功")
    void testRechargeSuccess() throws Exception {
        setAuthentication(adminUser);

        CarbonCoinRechargeRequest request = new CarbonCoinRechargeRequest();
        request.setAmount(new BigDecimal("1000.00"));
        request.setPaymentMethod(1);

        CarbonCoinAccountResponse updatedAccount = CarbonCoinAccountResponse.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("11000.00"))
                .totalRecharged(new BigDecimal("11000.00"))
                .totalSpent(BigDecimal.ZERO)
                .status(1)
                .build();

        when(carbonCoinService.recharge(eq(1L), any(CarbonCoinRechargeRequest.class)))
                .thenReturn(updatedAccount);

        mockMvc.perform(post("/carbon-coin/recharge")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.balance").value(11000.00));

        verify(carbonCoinService).recharge(eq(1L), any(CarbonCoinRechargeRequest.class));
    }

    @Test
    @DisplayName("碳币转账成功")
    void testTransferSuccess() throws Exception {
        setAuthentication(currentUser);

        CarbonCoinTransferRequest request = new CarbonCoinTransferRequest();
        request.setCounterpartId(2L);
        request.setAmount(new BigDecimal("500.00"));
        request.setRemark("测试转账");

        CarbonCoinAccountResponse updatedAccount = CarbonCoinAccountResponse.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("9500.00"))
                .totalRecharged(new BigDecimal("10000.00"))
                .totalSpent(new BigDecimal("500.00"))
                .status(1)
                .build();

        when(carbonCoinService.transfer(eq(1L), any(CarbonCoinTransferRequest.class)))
                .thenReturn(updatedAccount);

        mockMvc.perform(post("/carbon-coin/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.balance").value(9500.00));

        verify(carbonCoinService).transfer(eq(1L), any(CarbonCoinTransferRequest.class));
    }

    @Test
    @DisplayName("查询交易流水成功")
    void testGetTransactionsSuccess() throws Exception {
        setAuthentication(currentUser);

        Page<CarbonCoinTransaction> page = new PageImpl<>(List.of(transaction));
        when(carbonCoinService.getTransactions(eq(1L), eq(null), eq(1), eq(20))).thenReturn(page);

        mockMvc.perform(get("/carbon-coin/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        verify(carbonCoinService).getTransactions(1L, null, 1, 20);
    }

    @Test
    @DisplayName("查询交易流水-按类型筛选")
    void testGetTransactionsWithFilter() throws Exception {
        setAuthentication(currentUser);

        Page<CarbonCoinTransaction> page = new PageImpl<>(List.of(transaction));
        when(carbonCoinService.getTransactions(eq(1L), eq(1), eq(1), eq(10))).thenReturn(page);

        mockMvc.perform(get("/carbon-coin/transactions")
                        .param("userId", "1")
                        .param("txType", "1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(carbonCoinService).getTransactions(1L, 1, 1, 10);
    }
}
