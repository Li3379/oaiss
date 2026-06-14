package com.oaiss.chain.config;

import com.oaiss.chain.entity.RsaKeyPair;
import com.oaiss.chain.repository.RsaKeyPairRepository;
import com.oaiss.chain.util.AesGcmEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RsaKeyMigrationRunner unit tests")
class RsaKeyMigrationRunnerTest {

    @Mock
    private RsaKeyPairRepository rsaKeyPairRepository;

    @Mock
    private AesGcmEncryptor aesGcmEncryptor;

    @InjectMocks
    private RsaKeyMigrationRunner runner;

    @Test
    @DisplayName("migratePlaintextKeys should skip when nothing to migrate")
    void migratePlaintextKeys_shouldSkipWhenNoKeys() {
        when(rsaKeyPairRepository.findByEncryptedAndDeletedFalse(false)).thenReturn(List.of());

        runner.migratePlaintextKeys();

        verify(aesGcmEncryptor, never()).encrypt(anyString());
        verify(rsaKeyPairRepository, never()).save(any());
    }

    @Test
    @DisplayName("migratePlaintextKeys should continue after one item fails")
    void migratePlaintextKeys_shouldContinueAfterFailure() {
        RsaKeyPair first = RsaKeyPair.builder().userId(1L).privateKey("plain-1").encrypted(false).build();
        first.setId(11L);
        RsaKeyPair second = RsaKeyPair.builder().userId(2L).privateKey("plain-2").encrypted(false).build();
        second.setId(22L);

        when(rsaKeyPairRepository.findByEncryptedAndDeletedFalse(false)).thenReturn(List.of(first, second));
        when(aesGcmEncryptor.encrypt("plain-1")).thenThrow(new RuntimeException("encrypt failed"));
        when(aesGcmEncryptor.encrypt("plain-2")).thenReturn("cipher-2");

        runner.migratePlaintextKeys();

        verify(rsaKeyPairRepository, times(1)).save(second);
        assertEquals("cipher-2", second.getPrivateKey());
        assertTrue(second.getEncrypted());
    }

    @Test
    @DisplayName("migrateSingleKey should skip blank key")
    void migrateSingleKey_shouldSkipBlankKey() {
        RsaKeyPair keyPair = RsaKeyPair.builder().userId(1L).privateKey(" ").encrypted(false).build();
        keyPair.setId(10L);

        runner.migrateSingleKey(keyPair);

        verify(aesGcmEncryptor, never()).encrypt(anyString());
        verify(rsaKeyPairRepository, never()).save(any());
    }

    @Test
    @DisplayName("migrateSingleKey should encrypt and persist plaintext key")
    void migrateSingleKey_shouldEncryptAndPersist() {
        RsaKeyPair keyPair = RsaKeyPair.builder().userId(3L).privateKey("plain").encrypted(false).build();
        when(aesGcmEncryptor.encrypt("plain")).thenReturn("cipher");

        runner.migrateSingleKey(keyPair);

        assertEquals("cipher", keyPair.getPrivateKey());
        assertTrue(keyPair.getEncrypted());
        verify(rsaKeyPairRepository).save(keyPair);
    }

    @Test
    @DisplayName("migrateSingleKey should skip null private key")
    void migrateSingleKey_shouldSkipNullKey() {
        RsaKeyPair keyPair = RsaKeyPair.builder().userId(4L).privateKey(null).encrypted(false).build();
        keyPair.setId(20L);

        runner.migrateSingleKey(keyPair);

        verify(aesGcmEncryptor, never()).encrypt(anyString());
        verify(rsaKeyPairRepository, never()).save(any());
    }
}
