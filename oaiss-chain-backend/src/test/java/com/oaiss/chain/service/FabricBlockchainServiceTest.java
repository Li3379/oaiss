package com.oaiss.chain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.oaiss.chain.config.FabricProperties;
import com.oaiss.chain.exception.BlockchainException;
import io.grpc.Status;
import io.grpc.CallOptions;
import org.hyperledger.fabric.client.BlockEventsRequest;
import org.hyperledger.fabric.client.CloseableIterator;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.protos.common.Block;
import org.hyperledger.fabric.protos.common.BlockData;
import org.hyperledger.fabric.protos.common.BlockHeader;
import org.hyperledger.fabric.protos.common.ChannelHeader;
import org.hyperledger.fabric.protos.common.Envelope;
import org.hyperledger.fabric.protos.common.Header;
import org.hyperledger.fabric.protos.common.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FabricBlockchainServiceTest {

    @Mock
    private Contract carbonContract;

    @Mock
    private Network fabricNetwork;

    @Mock
    private BlockEventsRequest.Builder blockEventsRequestBuilder;

    @Mock
    private BlockEventsRequest blockEventsRequest;

    @Mock
    private CloseableIterator<Block> blockIterator;

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
        service = new FabricBlockchainService(carbonContract, fabricNetwork, props, objectMapper);

        lenient().when(fabricNetwork.newBlockEventsRequest()).thenReturn(blockEventsRequestBuilder);
        lenient().when(blockEventsRequestBuilder.startBlock(anyLong())).thenReturn(blockEventsRequestBuilder);
        lenient().when(blockEventsRequestBuilder.build()).thenReturn(blockEventsRequest);
        lenient().when(blockEventsRequest.getEvents(org.mockito.ArgumentMatchers.<UnaryOperator<CallOptions>>any()))
                .thenReturn(blockIterator);
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
    void queryTransaction_shouldReadFromHistoricalBlockEvents() {
        Block block = createBlock(9L, "tx123", "2026-05-31T00:00:00Z");
        when(blockIterator.hasNext()).thenReturn(true, false);
        when(blockIterator.next()).thenReturn(block);

        String result = service.queryTransaction("tx123");

        assertTrue(result.contains("\"txHash\":\"tx123\""));
        assertTrue(result.contains("\"blockNumber\":9"));
        verify(blockEventsRequestBuilder).startBlock(0L);
    }

    @Test
    void queryBlock_shouldReadSpecificBlockFromEventStream() {
        Block block = createBlock(5L, "tx-block-5", "2026-05-31T01:00:00Z");
        when(blockIterator.hasNext()).thenReturn(true, false);
        when(blockIterator.next()).thenReturn(block);

        String result = service.queryBlock(5L);

        assertTrue(result.contains("\"blockNumber\":5"));
        assertTrue(result.contains("\"txCount\":1"));
        verify(blockEventsRequestBuilder).startBlock(5L);
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
    void listTransactions_whenNoHistoricalBlocks_shouldReturnEmptyPage() {
        when(blockIterator.hasNext()).thenReturn(false);

        Page<Map<String, Object>> page = service.listTransactions(1, 10);

        assertEquals(0, page.getContent().size());
    }

    @Test
    void listTransactions_whenHistoricalBlocksExist_shouldReturnDescendingTransactionPage() {
        Block olderBlock = createBlock(2L, "tx-old", "2026-05-31T00:00:00Z");
        Block newerBlock = createBlock(3L, "tx-new", "2026-05-31T01:00:00Z");
        when(blockIterator.hasNext()).thenReturn(true, true, false);
        when(blockIterator.next()).thenReturn(olderBlock, newerBlock);

        Page<Map<String, Object>> page = service.listTransactions(1, 10);

        assertEquals(2, page.getContent().size());
        assertEquals("tx-new", page.getContent().get(0).get("txHash"));
        assertEquals("tx-old", page.getContent().get(1).get("txHash"));
    }

    @Test
    void listLatestBlocks_shouldReturnDescendingPage() {
        Block olderBlock = createBlock(1L, "tx-1", "2026-05-31T00:00:00Z");
        Block newerBlock = createBlock(2L, "tx-2", "2026-05-31T01:00:00Z");
        when(blockIterator.hasNext()).thenReturn(true, true, false);
        when(blockIterator.next()).thenReturn(olderBlock, newerBlock);

        Page<Map<String, Object>> page = service.listLatestBlocks(1, 10);

        assertEquals(2, page.getContent().size());
        assertEquals(2L, ((Number) page.getContent().get(0).get("blockNumber")).longValue());
        assertEquals(1L, ((Number) page.getContent().get(1).get("blockNumber")).longValue());
    }

    private Block createBlock(long blockNumber, String txId, String timestamp) {
        Instant instant = Instant.parse(timestamp);
        ChannelHeader channelHeader = ChannelHeader.newBuilder()
                .setChannelId("mychannel")
                .setTxId(txId)
                .setType(3)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(instant.getEpochSecond())
                        .setNanos(instant.getNano())
                        .build())
                .build();

        Header header = Header.newBuilder()
                .setChannelHeader(channelHeader.toByteString())
                .build();

        Payload payload = Payload.newBuilder()
                .setHeader(header)
                .build();

        Envelope envelope = Envelope.newBuilder()
                .setPayload(payload.toByteString())
                .build();

        return Block.newBuilder()
                .setHeader(BlockHeader.newBuilder()
                        .setNumber(blockNumber)
                        .setPreviousHash(ByteString.copyFromUtf8("prev-" + blockNumber))
                        .setDataHash(ByteString.copyFromUtf8("hash-" + blockNumber))
                        .build())
                .setData(BlockData.newBuilder()
                        .addData(envelope.toByteString())
                        .build())
                .build();
    }
}
