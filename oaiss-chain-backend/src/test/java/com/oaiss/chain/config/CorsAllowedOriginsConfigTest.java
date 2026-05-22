package com.oaiss.chain.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class CorsAllowedOriginsConfigTest {

    @Test
    void baseConfigAllowsLocal127Frontend() {
        assertAllowedOrigin("application.yml", "http://127.0.0.1:5173");
    }

    @Test
    void devConfigAllowsLocal127Frontend() {
        assertAllowedOrigin("application-dev.yml", "http://127.0.0.1:5173");
    }

    private void assertAllowedOrigin(String resourceName, String expectedOrigin) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(resourceName));

        Properties properties = yaml.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("app.cors.allowed-origins"))
                .as("%s should include %s", resourceName, expectedOrigin)
                .isNotNull()
                .satisfies(origins -> assertThat(Arrays.stream(origins.split(","))
                        .map(String::trim)
                        .toList())
                        .contains(expectedOrigin));
    }
}
