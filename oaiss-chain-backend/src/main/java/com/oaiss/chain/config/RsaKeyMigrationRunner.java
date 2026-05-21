package com.oaiss.chain.config;

import com.oaiss.chain.entity.RsaKeyPair;
import com.oaiss.chain.repository.RsaKeyPairRepository;
import com.oaiss.chain.util.AesGcmEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Migrates existing plaintext RSA private keys to AES-256-GCM encrypted storage.
 *
 * <p>Runs once on application startup. Reads all {@code rsa_key_pair} rows where
 * {@code encrypted = 0}, encrypts each private key with {@link AesGcmEncryptor},
 * and updates the row to {@code encrypted = 1}.</p>
 *
 * @author OAISS Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RsaKeyMigrationRunner {

    private final RsaKeyPairRepository rsaKeyPairRepository;
    private final AesGcmEncryptor aesGcmEncryptor;

    @EventListener(ApplicationReadyEvent.class)
    public void migratePlaintextKeys() {
        List<RsaKeyPair> unencryptedKeys = rsaKeyPairRepository.findByEncryptedAndDeletedFalse(0);

        if (unencryptedKeys.isEmpty()) {
            log.info("No unencrypted RSA private keys found -- migration skipped");
            return;
        }

        log.info("Found {} unencrypted RSA private key(s) -- starting migration", unencryptedKeys.size());

        int success = 0;
        for (RsaKeyPair keyPair : unencryptedKeys) {
            try {
                migrateSingleKey(keyPair);
                success++;
            } catch (Exception e) {
                log.error("Failed to migrate key id={}, userId={}: {}",
                        keyPair.getId(), keyPair.getUserId(), e.getMessage());
                // Continue with next key instead of aborting entire batch
            }
        }

        log.info("RSA private key migration complete -- {}/{} key(s) encrypted", success, unencryptedKeys.size());
    }

    @Transactional
    public void migrateSingleKey(RsaKeyPair keyPair) {
        String plaintextKey = keyPair.getPrivateKey();
        if (plaintextKey == null || plaintextKey.isBlank()) {
            log.warn("Skipping RSA key pair id={} with null/blank private key", keyPair.getId());
            return;
        }

        String encryptedKey = aesGcmEncryptor.encrypt(plaintextKey);
        keyPair.setPrivateKey(encryptedKey);
        keyPair.setEncrypted(1);
        rsaKeyPairRepository.save(keyPair);
    }
}
