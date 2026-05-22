package com.oaiss.chain.service;

import com.oaiss.chain.config.FabricProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.security.auth.x500.X500Principal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
@Profile("fabric")
@RequiredArgsConstructor
@Slf4j
public class FabricCAService {

    private final FabricProperties props;
    private final WebClient.Builder webClientBuilder;

    /**
     * Enrolls the gateway admin identity via Fabric CA REST API.
     * Generates an EC key pair, builds a CSR, posts to the CA /api/v1/enroll endpoint,
     * and returns the resulting Identity and Signer for use by FabricGatewayConfig.
     */
    public EnrollmentResult registerEnrollment() {
        try {
            FabricProperties.Ca caConfig = props.getCa();
            log.info("Enrolling gateway admin via Fabric CA at {}", caConfig.getEndpoint());

            // 1. Generate EC key pair using BouncyCastle
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(256);
            KeyPair keyPair = keyGen.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();

            // 2. Build PKCS10 CSR with adminName as CN
            X500Principal subject = new X500Principal("CN=" + caConfig.getAdminName());
            PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(
                    subject, keyPair.getPublic())
                    .build(signerForPrivateKey(privateKey));

            String csrPem = "-----BEGIN CERTIFICATE REQUEST-----\n" +
                    Base64.getEncoder().encodeToString(csr.getEncoded()) +
                    "\n-----END CERTIFICATE REQUEST-----";

            // 3. POST CSR to CA /api/v1/enroll with Basic auth
            String responseJson = webClientBuilder.build()
                    .post()
                    .uri(caConfig.getEndpoint() + "/api/v1/enroll")
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                            (caConfig.getAdminName() + ":" + caConfig.getAdminPassword()).getBytes()))
                    .bodyValue(csrPem)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 4. Parse JSON response and extract base64-encoded cert
            String certBase64 = extractCertFromResponse(responseJson);
            byte[] certBytes = Base64.getDecoder().decode(certBase64);

            // 5. Decode enrollment cert into X509Certificate
            X509Certificate certificate = (X509Certificate) java.security.cert.CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(new java.io.ByteArrayInputStream(certBytes));

            // 6. Construct Identity and Signer
            Identity identity = new X509Identity(props.getMspId(), certificate);
            Signer signer = Signers.newPrivateKeySigner(privateKey);

            log.info("CA enrollment successful for admin={}", caConfig.getAdminName());
            return new EnrollmentResult(identity, signer);
        } catch (Exception e) {
            log.error("CA enrollment failed: {}", e.getMessage());
            throw new RuntimeException("Fabric CA enrollment failed: " + e.getMessage(), e);
        }
    }

    private ContentSigner signerForPrivateKey(PrivateKey privateKey) throws Exception {
        return new JcaContentSignerBuilder("SHA256withECDSA")
                .build(privateKey);
    }

    private String extractCertFromResponse(String responseJson) {
        // Minimal JSON parsing to extract the "cert" field
        // The CA response format: { "result": { "Cert": "base64cert..." } }
        // or flat: { "cert": "base64cert..." }
        String certKey = "\"Cert\"";
        int certIdx = responseJson.indexOf(certKey);
        if (certIdx < 0) {
            certKey = "\"cert\"";
            certIdx = responseJson.indexOf(certKey);
        }
        if (certIdx < 0) {
            throw new RuntimeException("No certificate found in CA response");
        }

        // Find the value after the colon
        int colonIdx = responseJson.indexOf(':', certIdx);
        int quoteStart = responseJson.indexOf('"', colonIdx + 1);
        int quoteEnd = responseJson.indexOf('"', quoteStart + 1);
        return responseJson.substring(quoteStart + 1, quoteEnd);
    }

    /**
     * Record holding the CA enrollment result.
     * Not a JPA entity, not a DTO, not serializable.
     * Consumed only by FabricGatewayConfig.
     */
    public record EnrollmentResult(Identity identity, Signer signer) {
    }
}
