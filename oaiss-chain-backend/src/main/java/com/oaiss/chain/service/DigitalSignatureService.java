package com.oaiss.chain.service;

import com.oaiss.chain.dto.RsaKeyPairResponse;
import com.oaiss.chain.dto.SignatureResult;
import com.oaiss.chain.entity.RsaKeyPair;
import com.oaiss.chain.exception.BlockchainException;
import com.oaiss.chain.repository.RsaKeyPairRepository;
import com.oaiss.chain.util.RsaKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;

/**
 * 数字签名服务
 * Digital Signature Service
 * 
 * 提供RSA密钥对管理、碳报告签名、验签、数据加密解密功能
 * 用于碳核算审核流程中的数据安全保障
 * 
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalSignatureService {

    private final RsaKeyPairRepository rsaKeyPairRepository;

    /**
     * 密钥有效期（年）
     */
    private static final int KEY_EXPIRY_YEARS = 2;

    /**
     * 密钥状态：有效
     */
    private static final int KEY_STATUS_ACTIVE = 1;

    /**
     * 密钥状态：已失效
     */
    private static final int KEY_STATUS_REVOKED = 0;

    /**
     * 密钥状态：已过期
     */
    private static final int KEY_STATUS_EXPIRED = 2;

    // ==================== 密钥管理 ====================

    /**
     * 为用户生成RSA密钥对
     * Generate RSA key pair for user
     * 
     * @param userId 用户ID
     * @return 密钥对响应DTO（不含私钥）
     * @throws BlockchainException 如果密钥生成失败
     */
    @Transactional
    public RsaKeyPairResponse generateKeyPair(Long userId) {
        log.info("Generating RSA key pair for user: {}", userId);

        // 检查是否已有有效密钥对
        if (rsaKeyPairRepository.existsByUserIdAndDeletedFalse(userId)) {
            log.warn("User already has an active key pair: {}", userId);
            // 将旧密钥标记为失效
            revokeKeyPair(userId);
        }

        try {
            // 生成新的RSA密钥对
            KeyPair keyPair = RsaKeyUtil.generateKeyPair();
            String publicKeyBase64 = RsaKeyUtil.encodeKey(keyPair.getPublic());
            String privateKeyBase64 = RsaKeyUtil.encodeKey(keyPair.getPrivate());

            // 计算密钥版本（获取最新版本号）
            Integer latestVersion = rsaKeyPairRepository.findLatestByUserId(userId)
                    .map(RsaKeyPair::getKeyVersion)
                    .orElse(0);
            Integer newVersion = latestVersion + 1;

            // 创建密钥对实体
            RsaKeyPair rsaKeyPairEntity = RsaKeyPair.builder()
                    .userId(userId)
                    .publicKey(publicKeyBase64)
                    .privateKey(privateKeyBase64)
                    .keyStatus(KEY_STATUS_ACTIVE)
                    .expiresAt(LocalDateTime.now().plusYears(KEY_EXPIRY_YEARS))
                    .keyVersion(newVersion)
                    .keyUsage(3) // 通用用途
                    .build();

            // 保存到数据库
            rsaKeyPairEntity = rsaKeyPairRepository.save(rsaKeyPairEntity);
            log.info("RSA key pair generated successfully for user: {}, version: {}", userId, newVersion);

            return toResponse(rsaKeyPairEntity);
        } catch (Exception e) {
            log.error("RSA key pair generation failed for user {}: {}", userId, e.getMessage());
            throw BlockchainException.rsaKeyGenerationFailed(e.getMessage());
        }
    }

    /**
     * 撤销用户密钥对
     * Revoke user's key pair
     * 
     * @param userId 用户ID
     */
    @Transactional
    public void revokeKeyPair(Long userId) {
        log.info("Revoking RSA key pair for user: {}", userId);
        
        rsaKeyPairRepository.findLatestByUserId(userId)
                .ifPresent(keyPair -> {
                    keyPair.setKeyStatus(KEY_STATUS_REVOKED);
                    rsaKeyPairRepository.save(keyPair);
                    log.info("RSA key pair revoked for user: {}", userId);
                });
    }

    /**
     * 获取用户密钥对信息
     * Get user's key pair information
     * 
     * @param userId 用户ID
     * @return 密钥对响应DTO
     * @throws BlockchainException 如果密钥对不存在
     */
    public RsaKeyPairResponse getKeyPair(Long userId) {
        RsaKeyPair keyPair = rsaKeyPairRepository.findLatestByUserId(userId)
                .orElseThrow(() -> BlockchainException.rsaKeyPairNotFound(userId));

        // 检查密钥状态
        validateKeyStatus(keyPair);

        return toResponse(keyPair);
    }

    // ==================== 签名与验签 ====================

    /**
     * 对碳报告数据进行签名
     * Sign carbon report data
     * 
     * @param userId 用户ID（企业用户）
     * @param reportData 报告数据（JSON字符串）
     * @return 签名结果DTO
     * @throws BlockchainException 如果签名失败
     */
    public SignatureResult signReport(Long userId, String reportData) {
        log.info("Signing report data for user: {}", userId);

        // 获取用户密钥对
        RsaKeyPair keyPair = rsaKeyPairRepository.findLatestByUserId(userId)
                .orElseThrow(() -> BlockchainException.rsaKeyPairNotFound(userId));

        // 验证密钥状态
        validateKeyStatus(keyPair);

        try {
            // 解码私钥
            PrivateKey privateKey = RsaKeyUtil.decodePrivateKey(keyPair.getPrivateKey());

            // 使用SHA256withRSA签名
            String signature = RsaKeyUtil.sign(reportData, privateKey);

            log.info("Report data signed successfully for user: {}", userId);

            return SignatureResult.builder()
                    .signature(signature)
                    .algorithm(RsaKeyUtil.getSignatureAlgorithm())
                    .timestamp(LocalDateTime.now())
                    .signerId(userId)
                    .build();
        } catch (Exception e) {
            log.error("Report signing failed for user {}: {}", userId, e.getMessage());
            throw BlockchainException.rsaSignFailed(e.getMessage());
        }
    }

    /**
     * 验证碳报告签名
     * Verify carbon report signature
     * 
     * @param userId 用户ID（企业用户）
     * @param reportData 报告数据（JSON字符串）
     * @param signature 签名数据（Base64编码）
     * @return 验签是否成功
     * @throws BlockchainException 如果验签过程出错
     */
    public boolean verifySignature(Long userId, String reportData, String signature) {
        log.info("Verifying signature for user: {}", userId);

        // 获取用户密钥对
        RsaKeyPair keyPair = rsaKeyPairRepository.findLatestByUserId(userId)
                .orElseThrow(() -> BlockchainException.rsaKeyPairNotFound(userId));

        // 验证密钥状态（验签时允许使用已过期密钥，但记录警告）
        if (keyPair.getKeyStatus() == KEY_STATUS_REVOKED) {
            throw BlockchainException.rsaKeyRevoked(userId);
        }
        if (keyPair.getKeyStatus() == KEY_STATUS_EXPIRED || 
            (keyPair.getExpiresAt() != null && keyPair.getExpiresAt().isBefore(LocalDateTime.now()))) {
            log.warn("Using expired key for verification: userId={}", userId);
        }

        try {
            // 解码公钥
            PublicKey publicKey = RsaKeyUtil.decodePublicKey(keyPair.getPublicKey());

            // 验证签名
            boolean isValid = RsaKeyUtil.verify(reportData, signature, publicKey);

            log.info("Signature verification result for user {}: {}", userId, isValid);

            return isValid;
        } catch (Exception e) {
            log.error("Signature verification failed for user {}: {}", userId, e.getMessage());
            throw BlockchainException.rsaVerifyFailed(e.getMessage());
        }
    }

    // ==================== 加密与解密 ====================

    /**
     * 为审核员加密数据
     * Encrypt data for reviewer
     * 
     * 使用审核员的公钥加密数据，确保数据传输安全
     * 
     * @param data 待加密的数据
     * @param reviewerId 审核员用户ID
     * @return 加密后的数据（Base64编码）
     * @throws BlockchainException 如果加密失败
     */
    public String encryptForReviewer(String data, Long reviewerId) {
        log.info("Encrypting data for reviewer: {}", reviewerId);

        // 获取审核员密钥对
        RsaKeyPair keyPair = rsaKeyPairRepository.findLatestByUserId(reviewerId)
                .orElseThrow(() -> BlockchainException.rsaKeyPairNotFound(reviewerId));

        // 验证密钥状态
        validateKeyStatus(keyPair);

        try {
            // 解码公钥
            PublicKey publicKey = RsaKeyUtil.decodePublicKey(keyPair.getPublicKey());

            // 加密数据
            String encryptedData = RsaKeyUtil.encrypt(data, publicKey);

            log.info("Data encrypted successfully for reviewer: {}", reviewerId);

            return encryptedData;
        } catch (Exception e) {
            log.error("Data encryption failed for reviewer {}: {}", reviewerId, e.getMessage());
            throw BlockchainException.rsaEncryptFailed(e.getMessage());
        }
    }

    /**
     * 审核员解密来自企业的数据
     * Decrypt data from enterprise for reviewer
     * 
     * 使用审核员的私钥解密数据
     * 
     * @param encryptedData 加密的数据（Base64编码）
     * @param reviewerId 审核员用户ID
     * @return 解密后的原始数据
     * @throws BlockchainException 如果解密失败
     */
    public String decryptForReviewer(String encryptedData, Long reviewerId) {
        log.info("Decrypting data for reviewer: {}", reviewerId);

        // 获取审核员密钥对
        RsaKeyPair keyPair = rsaKeyPairRepository.findLatestByUserId(reviewerId)
                .orElseThrow(() -> BlockchainException.rsaKeyPairNotFound(reviewerId));

        // 验证密钥状态
        validateKeyStatus(keyPair);

        try {
            // 解码私钥
            PrivateKey privateKey = RsaKeyUtil.decodePrivateKey(keyPair.getPrivateKey());

            // 解密数据
            String decryptedData = RsaKeyUtil.decrypt(encryptedData, privateKey);

            log.info("Data decrypted successfully for reviewer: {}", reviewerId);

            return decryptedData;
        } catch (Exception e) {
            log.error("Data decryption failed for reviewer {}: {}", reviewerId, e.getMessage());
            throw BlockchainException.rsaDecryptFailed(e.getMessage());
        }
    }

    /**
     * 企业解密来自审核员的数据
     * Decrypt data from reviewer for enterprise
     * 
     * 使用企业的私钥解密数据
     * 
     * @param encryptedData 加密的数据（Base64编码）
     * @param enterpriseUserId 企业用户ID
     * @return 解密后的原始数据
     * @throws BlockchainException 如果解密失败
     */
    public String decryptForEnterprise(String encryptedData, Long enterpriseUserId) {
        log.info("Decrypting data for enterprise user: {}", enterpriseUserId);

        // 获取企业密钥对
        RsaKeyPair keyPair = rsaKeyPairRepository.findLatestByUserId(enterpriseUserId)
                .orElseThrow(() -> BlockchainException.rsaKeyPairNotFound(enterpriseUserId));

        // 验证密钥状态
        validateKeyStatus(keyPair);

        try {
            // 解码私钥
            PrivateKey privateKey = RsaKeyUtil.decodePrivateKey(keyPair.getPrivateKey());

            // 解密数据
            String decryptedData = RsaKeyUtil.decrypt(encryptedData, privateKey);

            log.info("Data decrypted successfully for enterprise user: {}", enterpriseUserId);

            return decryptedData;
        } catch (Exception e) {
            log.error("Data decryption failed for enterprise user {}: {}", enterpriseUserId, e.getMessage());
            throw BlockchainException.rsaDecryptFailed(e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 验证密钥状态
     * Validate key status
     * 
     * @param keyPair 密钥对
     * @throws BlockchainException 如果密钥无效
     */
    private void validateKeyStatus(RsaKeyPair keyPair) {
        // 检查密钥是否已失效
        if (keyPair.getKeyStatus() == KEY_STATUS_REVOKED) {
            throw BlockchainException.rsaKeyRevoked(keyPair.getUserId());
        }

        // 检查密钥是否已过期
        if (keyPair.getExpiresAt() != null && keyPair.getExpiresAt().isBefore(LocalDateTime.now())) {
            // 自动更新状态为已过期
            keyPair.setKeyStatus(KEY_STATUS_EXPIRED);
            rsaKeyPairRepository.save(keyPair);
            throw BlockchainException.rsaKeyExpired(keyPair.getUserId());
        }

        // 检查状态是否为已过期标记
        if (keyPair.getKeyStatus() == KEY_STATUS_EXPIRED) {
            throw BlockchainException.rsaKeyExpired(keyPair.getUserId());
        }
    }

    /**
     * Entity转Response
     * Convert entity to response DTO
     * 
     * 注意：不包含私钥信息
     */
    private RsaKeyPairResponse toResponse(RsaKeyPair keyPair) {
        String statusText;
        switch (keyPair.getKeyStatus()) {
            case KEY_STATUS_ACTIVE:
                statusText = "有效";
                break;
            case KEY_STATUS_REVOKED:
                statusText = "已失效";
                break;
            case KEY_STATUS_EXPIRED:
                statusText = "已过期";
                break;
            default:
                statusText = "未知";
        }

        return RsaKeyPairResponse.builder()
                .id(keyPair.getId())
                .userId(keyPair.getUserId())
                .publicKey(keyPair.getPublicKey())
                .createdAt(keyPair.getCreatedAt())
                .expiresAt(keyPair.getExpiresAt())
                .keyStatus(keyPair.getKeyStatus())
                .keyStatusText(statusText)
                .keyVersion(keyPair.getKeyVersion())
                .keyUsage(keyPair.getKeyUsage())
                .build();
    }
}