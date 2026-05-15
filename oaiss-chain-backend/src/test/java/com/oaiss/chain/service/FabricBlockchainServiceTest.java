package com.oaiss.chain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.config.FabricProperties;
import com.oaiss.chain.exception.BlockchainException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FabricBlockchainServiceTest {

    @Mock
    private Contract carbonContract;

    private FabricProperties props;
    private ObjectMapper objectMapper;
    private FabricBlockchainService service;

    @BeforeEach
    void setUp() {
        props = new FabricProperties();
        props.setChannelName("mychannel");
        props.setChaincodeName("carbon-chaincode");
        props.setMspId("Org1MSP");
        objectMapper = new ObjectMapper();
        service = new FabricBlockchainService(carbonContract, props, objectMapper);
    }

    @Test
    void commitReportToChain_shouldSubmitTransactionAndReturnTxHash() throws Exception {
        String jsonResponse = "{\"txHash\":\"abc123\",\"status\":\"OK\"}";
        when(carbonContract.submitTransaction(eq("CreateCarbonReport"), eq("1"), eq("reportData")))
                .thenReturn(jsonResponse.getBytes(StandardCharsets.UTF_8));

        String result = service.commitReportToChain(1L, "reportData");

        assertEquals("abc123", result);
        verify(carbonContract).submitTransaction("CreateCarbonReport", "1", "reportData");
    }

    @Test
    void commitTradeToChain_shouldSubmitTransactionAndReturnTxHash() throws Exception {
        String jsonResponse = "{\"txHash\":\"trade456\",\"status\":\"OK\"}";
        when(carbonContract.submitTransaction(eq("CreateTradeRecord"), eq("2"), eq("tradeData")))
                .thenReturn(jsonResponse.getBytes(StandardCharsets.UTF_8));

        String result = service.commitTradeToChain(2L, "tradeData");

        assertEquals("trade456", result);
        verify(carbonContract).submitTransaction("CreateTradeRecord", "2", "tradeData");
    }

    @Test
    void queryTransaction_shouldEvaluateTransaction() throws Exception {
        String expected = "{\"txId\":\"tx123\",\"data\":\"value\"}";
        when(carbonContract.evaluateTransaction(eq("GetTransactionByID"), eq("tx123")))
                .thenReturn(expected.getBytes(StandardCharsets.UTF_8));

        String result = service.queryTransaction("tx123");

        assertEquals(expected, result);
    }

    @Test
    void queryBlock_shouldEvaluateTransaction() throws Exception {
        String expected = "{\"blockNumber\":5,\"data\":\"blockdata\"}";
        when(carbonContract.evaluateTransaction(eq("QueryBlock"), eq("5")))
                .thenReturn(expected.getBytes(StandardCharsets.UTF_8));

        String result = service.queryBlock(5L);

        assertEquals(expected, result);
    }

    @Test
    void checkConnection_shouldReturnFabricStatus() {
        Map<String, Object> status = service.checkConnection();

        assertEquals(true, status.get("connected"));
        assertEquals("FABRIC", status.get("mode"));
        assertEquals("mychannel", status.get("channel"));
        assertEquals("Org1MSP", status.get("mspId"));
        assertEquals("carbon-chaincode", status.get("chaincode"));
    }

    @Test
    void invokeChaincode_shouldSubmitTransaction() throws Exception {
        when(carbonContract.submitTransaction(eq("func"), any(String[].class)))
                .thenReturn("result".getBytes(StandardCharsets.UTF_8));

        String result = service.invokeChaincode("mychannel", "cc", "func", "arg1");

        assertEquals("result", result);
    }

    @Test
    void queryChaincode_shouldEvaluateTransaction() throws Exception {
        when(carbonContract.evaluateTransaction(eq("func"), any(String[].class)))
                .thenReturn("queryResult".getBytes(StandardCharsets.UTF_8));

        String result = service.queryChaincode("mychannel", "cc", "func", "arg1");

        assertEquals("queryResult", result);
    }

    @Test
    void commitReportToChain_whenEndorseException_shouldThrowBlockchainException() throws Exception {
        when(carbonContract.submitTransaction(anyString(), any(String[].class)))
                .thenThrow(new EndorseException("endorsement failed", Status.INTERNAL.asRuntimeException()));

        assertThrows(BlockchainException.class, () ->
                service.commitReportToChain(1L, "data"));
    }

    @Test
    void commitReportToChain_whenRawResponse_shouldReturnRawString() throws Exception {
        when(carbonContract.submitTransaction(eq("CreateCarbonReport"), eq("1"), eq("data")))
                .thenReturn("raw-tx-hash".getBytes(StandardCharsets.UTF_8));

        String result = service.commitReportToChain(1L, "data");

        assertEquals("raw-tx-hash", result);
    }

    @Test
    void listTransactions_whenEmptyResponse_shouldReturnEmptyPage() throws Exception {
        when(carbonContract.evaluateTransaction(eq("ListTransactions"), eq("1"), eq("10")))
                .thenReturn("[]".getBytes(StandardCharsets.UTF_8));

        Page<Map<String, Object>> page = service.listTransactions(1, 10);

        assertEquals(0, page.getContent().size());
    }

    @Test
    void listTransactions_whenValidJsonArray_shouldReturnPage() throws Exception {
        String json = "[{\"txId\":\"tx1\",\"data\":\"d1\"},{\"txId\":\"tx2\",\"data\":\"d2\"}]";
        when(carbonContract.evaluateTransaction(eq("ListTransactions"), eq("1"), eq("10")))
                .thenReturn(json.getBytes(StandardCharsets.UTF_8));

        Page<Map<String, Object>> page = service.listTransactions(1, 10);

        assertEquals(2, page.getContent().size());
        assertEquals("tx1", page.getContent().get(0).get("txId"));
    }
}