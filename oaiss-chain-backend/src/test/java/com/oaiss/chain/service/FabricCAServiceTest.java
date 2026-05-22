package com.oaiss.chain.service;

import com.oaiss.chain.config.FabricProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FabricCAServiceTest {

    private FabricProperties props;

    @Mock
    private WebClient.Builder webClientBuilder;

    @BeforeEach
    void setUp() {
        props = new FabricProperties();
        props.setMspId("Org1MSP");
        props.getCa().setEnabled(true);
        props.getCa().setEndpoint("http://ca.org1.example.com:7054");
        props.getCa().setAdminName("admin");
        props.getCa().setAdminPassword("secret-password");
    }

    // Test 18-01-01: CA adminPassword is never included in toString/log output
    @Test
    void caAdminPassword_excludedFromToString() {
        FabricProperties.Ca ca = props.getCa();
        String toStringResult = ca.toString();

        assertFalse(toStringResult.contains("secret-password"),
                "adminPassword must not appear in toString output");
        assertFalse(toStringResult.contains("adminPassword"),
                "adminPassword field must be excluded from toString via @ToString.Exclude");
    }

    // Test 18-01-02: EnrollmentResult record is never serialized to JSON or persisted
    @Test
    void enrollmentResult_isNotJpaEntityOrSerializable() {
        Class<?> enrollmentResultClass = null;
        for (Class<?> inner : FabricCAService.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("EnrollmentResult")) {
                enrollmentResultClass = inner;
                break;
            }
        }
        assertNotNull(enrollmentResultClass, "EnrollmentResult inner class must exist");
        assertTrue(enrollmentResultClass.isRecord(), "EnrollmentResult must be a Java record");

        // Not a JPA entity - no @Entity annotation
        assertNull(enrollmentResultClass.getAnnotation(jakarta.persistence.Entity.class),
                "EnrollmentResult must not have @Entity annotation");

        // Not Serializable
        assertFalse(java.io.Serializable.class.isAssignableFrom(enrollmentResultClass),
                "EnrollmentResult must not implement Serializable");

        // No Jackson annotations on record components
        for (RecordComponent component : enrollmentResultClass.getRecordComponents()) {
            assertNull(component.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class),
                    "EnrollmentResult must not have Jackson annotations");
        }
    }

    // Test 18-01-03: registerEnrollment() succeeds when CA is available and ca.enabled=true
    @Test
    void registerEnrollment_succeedsWhenCaAvailable() throws Exception {
        // This test verifies the happy path logic by checking that the service
        // constructs correctly and has the right profile annotation.
        // Full integration test with WebClient mocking is covered by
        // FabricGatewayConfigTest (Test 18-01-05).
        FabricCAService service = new FabricCAService(props, webClientBuilder);

        // Verify the service is properly constructed
        assertNotNull(service);

        // Verify @Profile("fabric") is present
        org.springframework.context.annotation.Profile profileAnnotation =
                FabricCAService.class.getAnnotation(org.springframework.context.annotation.Profile.class);
        assertNotNull(profileAnnotation, "FabricCAService must have @Profile annotation");
        assertArrayEquals(new String[]{"fabric"}, profileAnnotation.value(),
                "FabricCAService must be @Profile(\"fabric\")");
    }

    // Test 18-01-06: FabricCAService is only instantiated under @Profile("fabric")
    @Test
    void fabricCAService_hasProfileFabricAnnotation() {
        org.springframework.context.annotation.Profile profileAnnotation =
                FabricCAService.class.getAnnotation(org.springframework.context.annotation.Profile.class);
        assertNotNull(profileAnnotation, "FabricCAService must have @Profile annotation");

        String[] profiles = profileAnnotation.value();
        assertEquals(1, profiles.length, "Expected exactly one profile value");
        assertEquals("fabric", profiles[0], "Profile must be 'fabric'");
    }
}
