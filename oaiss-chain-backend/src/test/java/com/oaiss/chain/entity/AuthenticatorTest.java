package com.oaiss.chain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Authenticator 实体测试
 * Authenticator Entity Tests
 */
class AuthenticatorTest {

    @Test
    @DisplayName("测试Builder创建对象")
    void testBuilder() {
        Authenticator authenticator = Authenticator.builder()
                .userId(100L)
                .orgName("Test Organization")
                .orgCode("ORG001")
                .address("Test Address")
                .contactPerson("John Doe")
                .contactPhone("1234567890")
                .certScope("[\"scope1\",\"scope2\"]")
                .status(1)
                .build();

        assertEquals(100L, authenticator.getUserId());
        assertEquals("Test Organization", authenticator.getOrgName());
        assertEquals("ORG001", authenticator.getOrgCode());
        assertEquals("Test Address", authenticator.getAddress());
        assertEquals("John Doe", authenticator.getContactPerson());
        assertEquals("1234567890", authenticator.getContactPhone());
        assertEquals("[\"scope1\",\"scope2\"]", authenticator.getCertScope());
        assertEquals(1, authenticator.getStatus());
    }

    @Test
    @DisplayName("测试无参构造函数")
    void testNoArgsConstructor() {
        Authenticator authenticator = new Authenticator();
        assertNull(authenticator.getId());
        assertNull(authenticator.getUserId());
        assertNull(authenticator.getOrgName());
        assertEquals(1, authenticator.getStatus()); // Default value
    }

    @Test
    @DisplayName("测试Setter和Getter")
    void testSetterGetter() {
        Authenticator authenticator = new Authenticator();
        
        authenticator.setId(2L);
        authenticator.setUserId(200L);
        authenticator.setOrgName("Updated Org");
        authenticator.setOrgCode("CODE002");
        authenticator.setAddress("Updated Address");
        authenticator.setContactPerson("Jane Doe");
        authenticator.setContactPhone("0987654321");
        authenticator.setCertScope("[\"newScope\"]");
        authenticator.setStatus(0);

        assertEquals(2L, authenticator.getId());
        assertEquals(200L, authenticator.getUserId());
        assertEquals("Updated Org", authenticator.getOrgName());
        assertEquals("CODE002", authenticator.getOrgCode());
        assertEquals("Updated Address", authenticator.getAddress());
        assertEquals("Jane Doe", authenticator.getContactPerson());
        assertEquals("0987654321", authenticator.getContactPhone());
        assertEquals("[\"newScope\"]", authenticator.getCertScope());
        assertEquals(0, authenticator.getStatus());
    }

    @Test
    @DisplayName("测试默认状态值")
    void testDefaultStatus() {
        Authenticator authenticator = Authenticator.builder()
                .userId(1L)
                .orgName("Test")
                .orgCode("CODE")
                .build();

        assertEquals(1, authenticator.getStatus());
    }

    @Test
    @DisplayName("测试ToString")
    void testToString() {
        Authenticator authenticator = Authenticator.builder()
                .orgName("Test Org")
                .orgCode("ORG001")
                .build();

        String str = authenticator.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }
}
