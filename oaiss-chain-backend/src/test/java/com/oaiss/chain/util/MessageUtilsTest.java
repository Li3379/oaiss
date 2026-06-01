package com.oaiss.chain.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MessageUtils unit tests")
class MessageUtilsTest {

    @AfterEach
    void tearDown() {
        MessageUtils utils = new MessageUtils();
        utils.setMessageSource(null);
    }

    @Test
    @DisplayName("should fall back to key and args when messageSource missing")
    void shouldFallbackWhenMessageSourceMissing() {
        assertEquals("error.key[arg1, 2]", MessageUtils.getMessage("error.key", "arg1", 2));
        assertEquals("plain.key", MessageUtils.getMessageEn("plain.key"));
    }

    @Test
    @DisplayName("should resolve message with explicit locale and helpers")
    void shouldResolveWithExplicitLocale() {
        MessageSource messageSource = mock(MessageSource.class);
        new MessageUtils().setMessageSource(messageSource);

        when(messageSource.getMessage(eq("hello"), any(), eq(Locale.US))).thenReturn("hello-en");
        when(messageSource.getMessage(eq("hello"), any(), eq(Locale.SIMPLIFIED_CHINESE))).thenReturn("hello-zh");

        assertEquals("hello-en", MessageUtils.getMessageEn("hello"));
        assertEquals("hello-zh", MessageUtils.getMessageZh("hello"));
    }

    @Test
    @DisplayName("should fall back when message lookup throws")
    void shouldFallbackWhenLookupFails() {
        MessageSource messageSource = mock(MessageSource.class);
        new MessageUtils().setMessageSource(messageSource);

        when(messageSource.getMessage(eq("missing"), any(), eq(Locale.US)))
                .thenThrow(new NoSuchMessageException("missing"));

        assertEquals("missing[x]", MessageUtils.getMessage("missing", Locale.US, "x"));
    }
}
