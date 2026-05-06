package com.oaiss.chain.service;

import com.oaiss.chain.dto.RsaKeyPairResponse;
import com.oaiss.chain.dto.SignatureResult;
import com.oaiss.chain.entity.RsaKeyPair;
import com.oaiss.chain.exception.BlockchainException;
import com.oaiss.chain.repository.RsaKeyPairRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DigitalSignatureService 单元测试
 * DigitalSignatureService Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class DigitalSignatureServiceTest {

    @Mock
    private RsaKeyPairRepository rsaKeyPairRepository;

    @InjectMocks
    private DigitalSignatureService digitalSignatureService;

    private RsaKeyPair testKeyPair;

    @BeforeEach
    void setUp() {
        testKeyPair = RsaKeyPair.builder()
                .userId(1L)
                .publicKey("test-public-key-base64")
                .privateKey("test-private-key-base64")
                .keyStatus(1) // Active
                .keyVersion(1)
                .keyUsage(3)
                .expiresAt(LocalDateTime.now().plusYears(2))
                .build();
        testKeyPair.setId(1L);
        testKeyPair.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("获取密钥对成功")
    void testGetKeyPairSuccess() {
        // Given
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When
        RsaKeyPairResponse response = digitalSignatureService.getKeyPair(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(1, response.getKeyStatus());
        verify(rsaKeyPairRepository, times(1)).findByUserIdAndDeletedFalse(1L);
    }

    @Test
    @DisplayName("获取密钥对失败-不存在")
    void testGetKeyPairFailNotFound() {
        // Given
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BlockchainException.class, () -> digitalSignatureService.getKeyPair(1L));
    }

    @Test
    @DisplayName("获取密钥对失败-已撤销")
    void testGetKeyPairFailRevoked() {
        // Given
        testKeyPair.setKeyStatus(0); // Revoked
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When & Then
        assertThrows(BlockchainException.class, () -> digitalSignatureService.getKeyPair(1L));
    }

    @Test
    @DisplayName("获取密钥对失败-已过期")
    void testGetKeyPairFailExpired() {
        // Given
        testKeyPair.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));
        when(rsaKeyPairRepository.save(any(RsaKeyPair.class))).thenReturn(testKeyPair);

        // When & Then
        assertThrows(BlockchainException.class, () -> digitalSignatureService.getKeyPair(1L));
    }

    @Test
    @DisplayName("撤销密钥对成功")
    void testRevokeKeyPairSuccess() {
        // Given
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));
        when(rsaKeyPairRepository.save(any(RsaKeyPair.class))).thenReturn(testKeyPair);

        // When
        digitalSignatureService.revokeKeyPair(1L);

        // Then
        verify(rsaKeyPairRepository, times(1)).save(any(RsaKeyPair.class));
    }

    @Test
    @DisplayName("撤销密钥对-不存在时无操作")
    void testRevokeKeyPairNotFound() {
        // Given
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        // When
        digitalSignatureService.revokeKeyPair(1L);

        // Then
        verify(rsaKeyPairRepository, never()).save(any());
    }

    @Test
    @DisplayName("生成密钥对-已有密钥时撤销旧密钥")
    void testGenerateKeyPairWithExistingKey() {
        // Given
        when(rsaKeyPairRepository.existsByUserIdAndDeletedFalse(1L)).thenReturn(true);
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));
        when(rsaKeyPairRepository.findLatestByUserId(1L)).thenReturn(Optional.of(testKeyPair));
        when(rsaKeyPairRepository.save(any(RsaKeyPair.class))).thenAnswer(invocation -> {
            RsaKeyPair keyPair = invocation.getArgument(0);
            keyPair.setId(2L);
            return keyPair;
        });

        // When
        RsaKeyPairResponse response = digitalSignatureService.generateKeyPair(1L);

        // Then
        assertNotNull(response);
        // Should have called save for both revoke and new key
        verify(rsaKeyPairRepository, atLeastOnce()).save(any(RsaKeyPair.class));
    }

    @Test
    @DisplayName("签名失败-密钥不存在")
    void testSignReportFailKeyNotFound() {
        // Given
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BlockchainException.class, () -> digitalSignatureService.signReport(1L, "test-data"));
    }

    @Test
    @DisplayName("验签失败-密钥不存在")
    void testVerifySignatureFailKeyNotFound() {
        // Given
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BlockchainException.class, () -> 
            digitalSignatureService.verifySignature(1L, "test-data", "test-signature"));
    }

    @Test
    @DisplayName("验签失败-密钥已撤销")
    void testVerifySignatureFailKeyRevoked() {
        // Given
        testKeyPair.setKeyStatus(0); // Revoked
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When & Then
        assertThrows(BlockchainException.class, () -> 
            digitalSignatureService.verifySignature(1L, "test-data", "test-signature"));
    }

    @Test
    @DisplayName("加密失败-密钥不存在")
    void testEncryptForReviewerFailKeyNotFound() {
        // Given
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BlockchainException.class, () -> 
            digitalSignatureService.encryptForReviewer("test-data", 1L));
    }

    @Test
    @DisplayName("解密失败-密钥不存在")
    void testDecryptForReviewerFailKeyNotFound() {
        // Given
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BlockchainException.class, () -> 
            digitalSignatureService.decryptForReviewer("encrypted-data", 1L));
    }

    @Test
    @DisplayName("生成密钥对成功-无现有密钥")
    void testGenerateKeyPairSuccessNoExistingKey() {
        // Given
        when(rsaKeyPairRepository.existsByUserIdAndDeletedFalse(1L)).thenReturn(false);
        when(rsaKeyPairRepository.findLatestByUserId(1L)).thenReturn(Optional.empty());
        when(rsaKeyPairRepository.save(any(RsaKeyPair.class))).thenAnswer(invocation -> {
            RsaKeyPair keyPair = invocation.getArgument(0);
            keyPair.setId(1L);
            return keyPair;
        });

        // When
        RsaKeyPairResponse response = digitalSignatureService.generateKeyPair(1L);

        // Then
        assertNotNull(response);
        assertNotNull(response.getPublicKey());
        assertEquals(1L, response.getUserId());
        assertEquals(1, response.getKeyStatus());
    }

    @Test
    @DisplayName("签名成功-使用真实密钥")
    void testSignReportSuccess() throws Exception {
        // Given - Generate a real key pair for testing
        java.security.KeyPair realKeyPair = com.oaiss.chain.util.RsaKeyUtil.generateKeyPair();
        String publicKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPublic());
        String privateKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPrivate());
        
        testKeyPair.setPublicKey(publicKeyBase64);
        testKeyPair.setPrivateKey(privateKeyBase64);
        
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When
        SignatureResult result = digitalSignatureService.signReport(1L, "test-report-data");

        // Then
        assertNotNull(result);
        assertNotNull(result.getSignature());
        assertEquals(1L, result.getSignerId());
        assertEquals("SHA256withRSA", result.getAlgorithm());
    }

    @Test
    @DisplayName("验签成功-使用真实密钥")
    void testVerifySignatureSuccess() throws Exception {
        // Given - Generate a real key pair and sign data
        java.security.KeyPair realKeyPair = com.oaiss.chain.util.RsaKeyUtil.generateKeyPair();
        String publicKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPublic());
        String privateKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPrivate());
        
        String testData = "test-report-data";
        String signature = com.oaiss.chain.util.RsaKeyUtil.sign(testData, realKeyPair.getPrivate());
        
        testKeyPair.setPublicKey(publicKeyBase64);
        testKeyPair.setPrivateKey(privateKeyBase64);
        
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When
        boolean isValid = digitalSignatureService.verifySignature(1L, testData, signature);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("验签失败-数据被篡改")
    void testVerifySignatureFailTamperedData() throws Exception {
        // Given - Generate a real key pair and sign data
        java.security.KeyPair realKeyPair = com.oaiss.chain.util.RsaKeyUtil.generateKeyPair();
        String publicKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPublic());
        String privateKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPrivate());
        
        String testData = "test-report-data";
        String signature = com.oaiss.chain.util.RsaKeyUtil.sign(testData, realKeyPair.getPrivate());
        
        testKeyPair.setPublicKey(publicKeyBase64);
        testKeyPair.setPrivateKey(privateKeyBase64);
        
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When - Verify with tampered data
        boolean isValid = digitalSignatureService.verifySignature(1L, "tampered-data", signature);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("验签成功-密钥已过期但允许验签")
    void testVerifySignatureWithExpiredKey() throws Exception {
        // Given - Generate a real key pair and sign data
        java.security.KeyPair realKeyPair = com.oaiss.chain.util.RsaKeyUtil.generateKeyPair();
        String publicKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPublic());
        String privateKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPrivate());
        
        String testData = "test-report-data";
        String signature = com.oaiss.chain.util.RsaKeyUtil.sign(testData, realKeyPair.getPrivate());
        
        testKeyPair.setPublicKey(publicKeyBase64);
        testKeyPair.setPrivateKey(privateKeyBase64);
        testKeyPair.setKeyStatus(2); // Expired status
        testKeyPair.setExpiresAt(LocalDateTime.now().minusDays(1));
        
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When
        boolean isValid = digitalSignatureService.verifySignature(1L, testData, signature);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("加密成功-使用真实密钥")
    void testEncryptForReviewerSuccess() throws Exception {
        // Given - Generate a real key pair
        java.security.KeyPair realKeyPair = com.oaiss.chain.util.RsaKeyUtil.generateKeyPair();
        String publicKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPublic());
        String privateKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPrivate());
        
        testKeyPair.setPublicKey(publicKeyBase64);
        testKeyPair.setPrivateKey(privateKeyBase64);
        
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When
        String encryptedData = digitalSignatureService.encryptForReviewer("sensitive-data", 1L);

        // Then
        assertNotNull(encryptedData);
        assertFalse(encryptedData.isEmpty());
    }

    @Test
    @DisplayName("解密成功-使用真实密钥")
    void testDecryptForReviewerSuccess() throws Exception {
        // Given - Generate a real key pair and encrypt data
        java.security.KeyPair realKeyPair = com.oaiss.chain.util.RsaKeyUtil.generateKeyPair();
        String publicKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPublic());
        String privateKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPrivate());
        
        String testData = "sensitive-data";
        String encryptedData = com.oaiss.chain.util.RsaKeyUtil.encrypt(testData, realKeyPair.getPublic());
        
        testKeyPair.setPublicKey(publicKeyBase64);
        testKeyPair.setPrivateKey(privateKeyBase64);
        
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When
        String decryptedData = digitalSignatureService.decryptForReviewer(encryptedData, 1L);

        // Then
        assertEquals(testData, decryptedData);
    }

    @Test
    @DisplayName("企业解密成功-使用真实密钥")
    void testDecryptForEnterpriseSuccess() throws Exception {
        // Given - Generate a real key pair and encrypt data
        java.security.KeyPair realKeyPair = com.oaiss.chain.util.RsaKeyUtil.generateKeyPair();
        String publicKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPublic());
        String privateKeyBase64 = com.oaiss.chain.util.RsaKeyUtil.encodeKey(realKeyPair.getPrivate());
        
        String testData = "enterprise-data";
        String encryptedData = com.oaiss.chain.util.RsaKeyUtil.encrypt(testData, realKeyPair.getPublic());
        
        testKeyPair.setPublicKey(publicKeyBase64);
        testKeyPair.setPrivateKey(privateKeyBase64);
        
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When
        String decryptedData = digitalSignatureService.decryptForEnterprise(encryptedData, 1L);

        // Then
        assertEquals(testData, decryptedData);
    }

    @Test
    @DisplayName("企业解密失败-密钥不存在")
    void testDecryptForEnterpriseFailKeyNotFound() {
        // Given
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BlockchainException.class, () -> 
            digitalSignatureService.decryptForEnterprise("encrypted-data", 1L));
    }

    @Test
    @DisplayName("企业解密失败-密钥已撤销")
    void testDecryptForEnterpriseFailKeyRevoked() {
        // Given
        testKeyPair.setKeyStatus(0); // Revoked
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When & Then
        assertThrows(BlockchainException.class, () -> 
            digitalSignatureService.decryptForEnterprise("encrypted-data", 1L));
    }

    @Test
    @DisplayName("签名失败-密钥已撤销")
    void testSignReportFailKeyRevoked() {
        // Given
        testKeyPair.setKeyStatus(0); // Revoked
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When & Then
        assertThrows(BlockchainException.class, () -> digitalSignatureService.signReport(1L, "test-data"));
    }

    @Test
    @DisplayName("加密失败-密钥已撤销")
    void testEncryptForReviewerFailKeyRevoked() {
        // Given
        testKeyPair.setKeyStatus(0); // Revoked
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When & Then
        assertThrows(BlockchainException.class, () -> 
            digitalSignatureService.encryptForReviewer("test-data", 1L));
    }

    @Test
    @DisplayName("解密失败-密钥已撤销")
    void testDecryptForReviewerFailKeyRevoked() {
        // Given
        testKeyPair.setKeyStatus(0); // Revoked
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When & Then
        assertThrows(BlockchainException.class, () -> 
            digitalSignatureService.decryptForReviewer("encrypted-data", 1L));
    }

    @Test
    @DisplayName("获取密钥对-状态文本已失效")
    void testGetKeyPairStatusRevoked() {
        // Given
        testKeyPair.setKeyStatus(0); // Revoked
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When & Then
        assertThrows(BlockchainException.class, () -> digitalSignatureService.getKeyPair(1L));
    }

    @Test
    @DisplayName("获取密钥对-状态已过期标记")
    void testGetKeyPairStatusExpiredMarked() {
        // Given
        testKeyPair.setKeyStatus(2); // Expired status
        when(rsaKeyPairRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testKeyPair));

        // When & Then
        assertThrows(BlockchainException.class, () -> digitalSignatureService.getKeyPair(1L));
    }
}
