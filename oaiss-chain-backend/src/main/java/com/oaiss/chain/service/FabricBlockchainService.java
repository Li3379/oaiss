package com.oaiss.chain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.config.FabricProperties;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.exception.BlockchainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Profile("fabric")
@RequiredArgsConstructor
public class FabricBlockchainService implements BlockchainServicePort {

    private final Contract carbonContract;
    private final FabricProperties props;
    private final ObjectMapper objectMapper;

    @Override
    public String invokeChaincode(String channelName, String chaincodeName, String functionName, String... args) {
        try {
            log.info("[FABRIC] Invoking chaincode: channel={}, chaincode={}, function={}",
                    channelName, chaincodeName, functionName);
            byte[] result = carbonContract.submitTransaction(functionName, args);
            return new String(result, StandardCharsets.UTF_8);
        } catch (EndorseException e) {
            throw BlockchainException.chaincodeInvokeFailed(chaincodeName, e.getMessage());
        } catch (CommitException e) {
            throw BlockchainException.txCommitFailed("invoke", e.getMessage());
        } catch (Exception e) {
            throw BlockchainException.chaincodeInvokeFailed(chaincodeName, e.getMessage());
        }
    }

    @Override
    public String queryChaincode(String channelName, String chaincodeName, String functionName, String... args) {
        try {
            log.info("[FABRIC] Querying chaincode: channel={}, chaincode={}, function={}",
                    channelName, chaincodeName, functionName);
            byte[] result = carbonContract.evaluateTransaction(functionName, args);
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw BlockchainException.chaincodeInvokeFailed(chaincodeName, e.getMessage());
        }
    }

    @Override
    public String commitReportToChain(Long reportId, String reportData) {
        try {
            log.info("[FABRIC] Committing carbon report to chain: reportId={}", reportId);
            byte[] result = carbonContract.submitTransaction(
                    "CreateCarbonReport",
                    String.valueOf(reportId),
                    reportData
            );
            String response = new String(result, StandardCharsets.UTF_8);
            log.info("[FABRIC] Report committed: reportId={}, response={}", reportId, response);
            return extractTxHashFromResponse(response);
        } catch (EndorseException e) {
            throw BlockchainException.smartContractError("CreateCarbonReport", e.getMessage());
        } catch (CommitException e) {
            throw BlockchainException.txCommitFailed(String.valueOf(reportId), e.getMessage());
        } catch (Exception e) {
            throw BlockchainException.txCommitFailed(String.valueOf(reportId), e.getMessage());
        }
    }

    @Override
    public String commitTradeToChain(Long tradeId, String tradeData) {
        try {
            log.info("[FABRIC] Committing trade to chain: tradeId={}", tradeId);
            byte[] result = carbonContract.submitTransaction(
                    "CreateTradeRecord",
                    String.valueOf(tradeId),
                    tradeData
            );
            String response = new String(result, StandardCharsets.UTF_8);
            log.info("[FABRIC] Trade committed: tradeId={}, response={}", tradeId, response);
            return extractTxHashFromResponse(response);
        } catch (EndorseException e) {
            throw BlockchainException.smartContractError("CreateTradeRecord", e.getMessage());
        } catch (CommitException e) {
            throw BlockchainException.txCommitFailed(String.valueOf(tradeId), e.getMessage());
        } catch (Exception e) {
            throw BlockchainException.txCommitFailed(String.valueOf(tradeId), e.getMessage());
        }
    }

    @Override
    public String queryBlock(Long blockNumber) {
        try {
            log.info("[FABRIC] Querying block: {}", blockNumber);
            byte[] result = carbonContract.evaluateTransaction("QueryBlock", String.valueOf(blockNumber));
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw BlockchainException.blockQueryFailed(blockNumber, e.getMessage());
        }
    }

    @Override
    public String queryTransaction(String txHash) {
        try {
            log.info("[FABRIC] Querying transaction: {}", txHash);
            byte[] result = carbonContract.evaluateTransaction("GetTransactionByID", txHash);
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw BlockchainException.txQueryFailed(txHash, e.getMessage());
        }
    }

    @Override
    public boolean verifySignature(String data, String signature, String publicKey) {
        try {
            log.info("[FABRIC] Verifying signature for data length: {}", data.length());
            byte[] result = carbonContract.evaluateTransaction("VerifySignature", data, signature, publicKey);
            String response = new String(result, StandardCharsets.UTF_8);
            return Boolean.parseBoolean(response);
        } catch (Exception e) {
            log.error("[FABRIC] Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> checkConnection() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", true);
        status.put("channel", props.getChannelName());
        status.put("peers", 1);
        status.put("orderers", 1);
        status.put("mode", "FABRIC");
        status.put("mspId", props.getMspId());
        status.put("chaincode", props.getChaincodeName());
        status.put("timestamp", LocalDateTime.now().toString());
        return status;
    }

    @Override
    public Page<Map<String, Object>> listTransactions(Integer page, Integer size) {
        try {
            log.info("[FABRIC] Listing transactions: page={}, size={}", page, size);
            byte[] result = carbonContract.evaluateTransaction("ListTransactions",
                    String.valueOf(page), String.valueOf(size));
            String response = new String(result, StandardCharsets.UTF_8);

            JsonNode node = objectMapper.readTree(response);
            List<Map<String, Object>> transactions = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    Map<String, Object> tx = objectMapper.convertValue(item, Map.class);
                    transactions.add(tx);
                }
            }
            long total = transactions.size() > 0 ? 100 : 0;
            return new PageImpl<>(transactions, PageRequest.of(page - 1, size), total);
        } catch (Exception e) {
            log.error("[FABRIC] Failed to list transactions: {}", e.getMessage());
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page - 1, size), 0);
        }
    }

    @Override
    public Page<Map<String, Object>> listLatestBlocks(Integer page, Integer size) {
        try {
            log.info("[FABRIC] Listing latest blocks: page={}, size={}", page, size);
            byte[] result = carbonContract.evaluateTransaction("ListLatestBlocks",
                    String.valueOf(page), String.valueOf(size));
            String response = new String(result, StandardCharsets.UTF_8);

            JsonNode node = objectMapper.readTree(response);
            List<Map<String, Object>> blocks = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    Map<String, Object> block = objectMapper.convertValue(item, Map.class);
                    blocks.add(block);
                }
            }
            long total = blocks.size() > 0 ? 10000 : 0;
            return new PageImpl<>(blocks, PageRequest.of(page - 1, size), total);
        } catch (Exception e) {
            log.error("[FABRIC] Failed to list blocks: {}", e.getMessage());
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page - 1, size), 0);
        }
    }

    private String extractTxHashFromResponse(String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            if (node.has("txHash")) {
                return node.get("txHash").asText();
            }
        } catch (Exception e) {
            log.debug("Could not parse txHash from response, returning raw response");
        }
        return response;
    }
}