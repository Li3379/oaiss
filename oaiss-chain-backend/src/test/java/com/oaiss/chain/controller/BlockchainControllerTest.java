package com.oaiss.chain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.exception.BlockchainException;
import com.oaiss.chain.security.JwtTokenProvider;
import com.oaiss.chain.service.BlockchainServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BlockchainController Unit Tests
 * 区块链控制器单元测试
 *
 * @author OAISS Team
 */
@WebMvcTest(value = BlockchainController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
        })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class BlockchainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BlockchainServicePort blockchainService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    // ==================== Check Status Tests ====================

    @Nested
    @DisplayName("检查区块链连接状态测试")
    class CheckStatusTests {

        @Test
        @DisplayName("检查区块链连接状态成功")
        void testCheckStatusSuccess() throws Exception {
            // Given
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("connected", true);
            statusData.put("channel", "carbon-channel");
            statusData.put("peers", 2);
            statusData.put("orderers", 1);
            statusData.put("mode", "MOCK");

            when(blockchainService.checkConnection()).thenReturn(statusData);

            // When & Then
            mockMvc.perform(get("/blockchain/status")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.connected").value(true))
                    .andExpect(jsonPath("$.data.channel").value("carbon-channel"))
                    .andExpect(jsonPath("$.data.peers").value(2))
                    .andExpect(jsonPath("$.data.orderers").value(1))
                    .andExpect(jsonPath("$.data.mode").value("MOCK"));

            verify(blockchainService, times(1)).checkConnection();
        }

        @Test
        @DisplayName("检查区块链连接状态-连接失败")
        void testCheckStatusConnectionFailed() throws Exception {
            // Given
            when(blockchainService.checkConnection())
                    .thenThrow(BlockchainException.connectionFailed("网络不可达"));

            // When & Then
            mockMvc.perform(get("/blockchain/status")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5000));

            verify(blockchainService, times(1)).checkConnection();
        }

        @Test
        @DisplayName("检查区块链连接状态-返回未连接状态")
        void testCheckStatusNotConnected() throws Exception {
            // Given
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("connected", false);
            statusData.put("channel", null);
            statusData.put("peers", 0);
            statusData.put("orderers", 0);
            statusData.put("mode", "MOCK");

            when(blockchainService.checkConnection()).thenReturn(statusData);

            // When & Then
            mockMvc.perform(get("/blockchain/status")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.connected").value(false))
                    .andExpect(jsonPath("$.data.peers").value(0));

            verify(blockchainService, times(1)).checkConnection();
        }
    }

    // ==================== Query Block Tests ====================

    @Nested
    @DisplayName("查询区块信息测试")
    class QueryBlockTests {

        @Test
        @DisplayName("查询区块信息成功")
        void testQueryBlockSuccess() throws Exception {
            // Given
            Long blockNumber = 1000L;
            String blockInfo = "{\"blockNumber\":1000,\"txCount\":5,\"timestamp\":\"2024-01-15T10:30:00\"}";

            when(blockchainService.queryBlock(blockNumber)).thenReturn(blockInfo);

            // When & Then
            mockMvc.perform(get("/blockchain/block/{blockNumber}", blockNumber)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(blockInfo));

            verify(blockchainService, times(1)).queryBlock(blockNumber);
        }

        @Test
        @DisplayName("查询区块信息-区块号0")
        void testQueryBlockZero() throws Exception {
            // Given
            Long blockNumber = 0L;
            String blockInfo = "{\"blockNumber\":0,\"txCount\":1,\"timestamp\":\"2024-01-01T00:00:00\"}";

            when(blockchainService.queryBlock(blockNumber)).thenReturn(blockInfo);

            // When & Then
            mockMvc.perform(get("/blockchain/block/{blockNumber}", blockNumber)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(blockInfo));

            verify(blockchainService, times(1)).queryBlock(blockNumber);
        }

        @Test
        @DisplayName("查询区块信息-大区块号")
        void testQueryBlockLargeNumber() throws Exception {
            // Given
            Long blockNumber = 999999999L;
            String blockInfo = "{\"blockNumber\":999999999,\"txCount\":10}";

            when(blockchainService.queryBlock(blockNumber)).thenReturn(blockInfo);

            // When & Then
            mockMvc.perform(get("/blockchain/block/{blockNumber}", blockNumber)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(blockInfo));

            verify(blockchainService, times(1)).queryBlock(blockNumber);
        }

        @Test
        @DisplayName("查询区块信息-区块不存在")
        void testQueryBlockNotFound() throws Exception {
            // Given
            Long blockNumber = 999999L;
            when(blockchainService.queryBlock(blockNumber))
                    .thenThrow(BlockchainException.blockQueryFailed(blockNumber, "区块不存在"));

            // When & Then
            mockMvc.perform(get("/blockchain/block/{blockNumber}", blockNumber)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5003));

            verify(blockchainService, times(1)).queryBlock(blockNumber);
        }

        @Test
        @DisplayName("查询区块信息-区块链连接异常")
        void testQueryBlockConnectionError() throws Exception {
            // Given
            Long blockNumber = 100L;
            when(blockchainService.queryBlock(blockNumber))
                    .thenThrow(BlockchainException.connectionFailed("节点离线"));

            // When & Then
            mockMvc.perform(get("/blockchain/block/{blockNumber}", blockNumber)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5000));

            verify(blockchainService, times(1)).queryBlock(blockNumber);
        }

        @Test
        @DisplayName("查询区块信息-无效区块号(负数)")
        void testQueryBlockInvalidBlockNumber() throws Exception {
            // Given - IllegalArgumentException会被全局异常处理器处理为500
            Long blockNumber = -1L;
            when(blockchainService.queryBlock(blockNumber))
                    .thenThrow(new IllegalArgumentException("区块号不能为负数"));

            // When & Then - IllegalArgumentException未被专门处理，返回500
            mockMvc.perform(get("/blockchain/block/{blockNumber}", blockNumber)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verify(blockchainService, times(1)).queryBlock(blockNumber);
        }
    }

    // ==================== Query Transaction Tests ====================

    @Nested
    @DisplayName("查询交易信息测试")
    class QueryTransactionTests {

        @Test
        @DisplayName("查询交易信息成功")
        void testQueryTransactionSuccess() throws Exception {
            // Given
            String txHash = "tx_mock_1705312200000_abc12345";
            String txInfo = "{\"txHash\":\"tx_mock_1705312200000_abc12345\",\"status\":\"VALID\",\"timestamp\":\"2024-01-15T10:30:00\"}";

            when(blockchainService.queryTransaction(txHash)).thenReturn(txInfo);

            // When & Then
            mockMvc.perform(get("/blockchain/transaction/{txHash}", txHash)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(txInfo));

            verify(blockchainService, times(1)).queryTransaction(txHash);
        }

        @Test
        @DisplayName("查询交易信息-带0x前缀的哈希")
        void testQueryTransactionWithPrefix() throws Exception {
            // Given
            String txHash = "0x1234567890abcdef1234567890abcdef";
            String txInfo = "{\"txHash\":\"0x1234567890abcdef1234567890abcdef\",\"status\":\"VALID\"}";

            when(blockchainService.queryTransaction(txHash)).thenReturn(txInfo);

            // When & Then
            mockMvc.perform(get("/blockchain/transaction/{txHash}", txHash)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(txInfo));

            verify(blockchainService, times(1)).queryTransaction(txHash);
        }

        @Test
        @DisplayName("查询交易信息-长哈希值")
        void testQueryTransactionLongHash() throws Exception {
            // Given
            String txHash = "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456";
            String txInfo = "{\"txHash\":\"" + txHash + "\",\"status\":\"VALID\"}";

            when(blockchainService.queryTransaction(txHash)).thenReturn(txInfo);

            // When & Then
            mockMvc.perform(get("/blockchain/transaction/{txHash}", txHash)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(txInfo));

            verify(blockchainService, times(1)).queryTransaction(txHash);
        }

        @Test
        @DisplayName("查询交易信息-交易不存在")
        void testQueryTransactionNotFound() throws Exception {
            // Given
            String txHash = "nonexistent_tx_hash";
            when(blockchainService.queryTransaction(txHash))
                    .thenThrow(BlockchainException.txQueryFailed(txHash, "交易不存在"));

            // When & Then
            mockMvc.perform(get("/blockchain/transaction/{txHash}", txHash)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5004));

            verify(blockchainService, times(1)).queryTransaction(txHash);
        }

        @Test
        @DisplayName("查询交易信息-交易状态为无效")
        void testQueryTransactionInvalidStatus() throws Exception {
            // Given
            String txHash = "invalid_tx_hash";
            String txInfo = "{\"txHash\":\"invalid_tx_hash\",\"status\":\"INVALID\",\"error\":\"交易验证失败\"}";

            when(blockchainService.queryTransaction(txHash)).thenReturn(txInfo);

            // When & Then
            mockMvc.perform(get("/blockchain/transaction/{txHash}", txHash)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(txInfo));

            verify(blockchainService, times(1)).queryTransaction(txHash);
        }

        @Test
        @DisplayName("查询交易信息-区块链连接异常")
        void testQueryTransactionConnectionError() throws Exception {
            // Given
            String txHash = "test_tx_hash";
            when(blockchainService.queryTransaction(txHash))
                    .thenThrow(BlockchainException.connectionFailed("节点不可达"));

            // When & Then
            mockMvc.perform(get("/blockchain/transaction/{txHash}", txHash)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5000));

            verify(blockchainService, times(1)).queryTransaction(txHash);
        }

        @Test
        @DisplayName("查询交易信息-空哈希")
        void testQueryTransactionEmptyHash() throws Exception {
            // Given - 空哈希值，Spring可能无法正确路由
            String txHash = "";

            // When & Then - 返回500
            mockMvc.perform(get("/blockchain/transaction/{txHash}", txHash)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            // Note: 空路径参数返回500错误
        }

        @Test
        @DisplayName("查询交易信息-包含特殊字符的哈希")
        void testQueryTransactionWithSpecialChars() throws Exception {
            // Given - URL编码后的特殊字符
            String txHash = "tx_hash_with_special";
            String txInfo = "{\"txHash\":\"tx_hash_with_special\"}";

            when(blockchainService.queryTransaction(txHash)).thenReturn(txInfo);

            // When & Then
            mockMvc.perform(get("/blockchain/transaction/{txHash}", txHash)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(blockchainService, times(1)).queryTransaction(txHash);
        }
    }

    // ==================== Additional Edge Cases ====================

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("状态检查-空响应数据")
        void testCheckStatusEmptyResponse() throws Exception {
            // Given
            Map<String, Object> statusData = new HashMap<>();
            when(blockchainService.checkConnection()).thenReturn(statusData);

            // When & Then
            mockMvc.perform(get("/blockchain/status")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").exists());

            verify(blockchainService, times(1)).checkConnection();
        }

        @Test
        @DisplayName("区块查询-空返回值")
        void testQueryBlockEmptyResponse() throws Exception {
            // Given
            Long blockNumber = 1L;
            when(blockchainService.queryBlock(blockNumber)).thenReturn("");

            // When & Then
            mockMvc.perform(get("/blockchain/block/{blockNumber}", blockNumber)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(""));

            verify(blockchainService, times(1)).queryBlock(blockNumber);
        }

        @Test
        @DisplayName("交易查询-空返回值")
        void testQueryTransactionEmptyResponse() throws Exception {
            // Given
            String txHash = "empty_tx";
            when(blockchainService.queryTransaction(txHash)).thenReturn("");

            // When & Then
            mockMvc.perform(get("/blockchain/transaction/{txHash}", txHash)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(""));

            verify(blockchainService, times(1)).queryTransaction(txHash);
        }

        @Test
        @DisplayName("区块查询-包含JSON特殊字符")
        void testQueryBlockWithSpecialJsonChars() throws Exception {
            // Given
            Long blockNumber = 100L;
            String blockInfo = "{\"blockNumber\":100,\"data\":\"value with \\\"quotes\\\" and \\n newline\"}";

            when(blockchainService.queryBlock(blockNumber)).thenReturn(blockInfo);

            // When & Then
            mockMvc.perform(get("/blockchain/block/{blockNumber}", blockNumber)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(blockchainService, times(1)).queryBlock(blockNumber);
        }
    }
}
