package com.oaiss.chain.entity;

import jakarta.persistence.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Optimistic lock tests for financial entities (CON-02)
 * Verifies that Enterprise, CarbonCoinAccount, and AuctionOrder
 * have @Version fields for optimistic locking.
 */
class OptimisticLockTest {

    @Test
    @DisplayName("Enterprise实体应有@Version注解的version字段")
    void enterpriseShouldHaveVersionField() throws NoSuchFieldException {
        // When
        Field versionField = Enterprise.class.getDeclaredField("version");

        // Then
        assertThat(versionField.getType())
                .as("version field must be Long type for JPA @Version")
                .isEqualTo(Long.class);
        assertThat(versionField.getAnnotation(Version.class))
                .as("version field must have @Version annotation")
                .isNotNull();
    }

    @Test
    @DisplayName("CarbonCoinAccount实体应有@Version注解的version字段")
    void carbonCoinAccountShouldHaveVersionField() throws NoSuchFieldException {
        // When
        Field versionField = CarbonCoinAccount.class.getDeclaredField("version");

        // Then
        assertThat(versionField.getType())
                .as("version field must be Long type for JPA @Version")
                .isEqualTo(Long.class);
        assertThat(versionField.getAnnotation(Version.class))
                .as("version field must have @Version annotation")
                .isNotNull();
    }

    @Test
    @DisplayName("AuctionOrder实体应有@Version注解的version字段")
    void auctionOrderShouldHaveVersionField() throws NoSuchFieldException {
        // When
        Field versionField = AuctionOrder.class.getDeclaredField("version");

        // Then
        assertThat(versionField.getType())
                .as("version field must be Long type for JPA @Version")
                .isEqualTo(Long.class);
        assertThat(versionField.getAnnotation(Version.class))
                .as("version field must have @Version annotation")
                .isNotNull();
    }

    @Test
    @DisplayName("Enterprise的version字段应有@Builder.Default注解")
    void enterpriseVersionShouldHaveBuilderDefault() throws NoSuchFieldException {
        // When
        Field versionField = Enterprise.class.getDeclaredField("version");

        // Then
        assertThat(versionField.getAnnotation(lombok.Builder.Default.class))
                .as("version field must have @Builder.Default so builder sets version=0L instead of null")
                .isNotNull();
    }

    @Test
    @DisplayName("CarbonCoinAccount的version字段应有@Builder.Default注解")
    void carbonCoinAccountVersionShouldHaveBuilderDefault() throws NoSuchFieldException {
        // When
        Field versionField = CarbonCoinAccount.class.getDeclaredField("version");

        // Then
        assertThat(versionField.getAnnotation(lombok.Builder.Default.class))
                .as("version field must have @Builder.Default so builder sets version=0L instead of null")
                .isNotNull();
    }

    @Test
    @DisplayName("AuctionOrder的version字段应有@Builder.Default注解")
    void auctionOrderVersionShouldHaveBuilderDefault() throws NoSuchFieldException {
        // When
        Field versionField = AuctionOrder.class.getDeclaredField("version");

        // Then
        assertThat(versionField.getAnnotation(lombok.Builder.Default.class))
                .as("version field must have @Builder.Default so builder sets version=0L instead of null")
                .isNotNull();
    }
}
