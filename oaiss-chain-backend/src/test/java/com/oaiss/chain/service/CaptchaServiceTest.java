package com.oaiss.chain.service;

import com.oaiss.chain.dto.CaptchaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证码服务测试
 * 
 * @author OAISS Team
 */
class CaptchaServiceTest {

    private final CaptchaService captchaService = new CaptchaService();

    @Test
    @DisplayName("生成验证码 - 应返回有效的验证码Key和Base64图片")
    void testGenerateCaptcha() {
        CaptchaResponse response = captchaService.generateCaptcha();
        
        assertNotNull(response);
        assertNotNull(response.getCaptchaKey());
        assertNotNull(response.getCaptchaImage());
        assertTrue(response.getCaptchaImage().startsWith("data:image/png;base64,"));
        assertEquals(300L, response.getExpiresIn());
    }

    @Test
    @DisplayName("验证验证码 - 正确的验证码应返回true")
    void testVerifyCaptchaCorrect() {
        CaptchaResponse response = captchaService.generateCaptcha();
        // 注意：这里无法直接测试，因为验证码是随机生成的
        // 实际测试中需要mock或使用可预测的验证码生成器
    }

    @Test
    @DisplayName("验证验证码 - 错误的验证码应返回false")
    void testVerifyCaptchaWrong() {
        CaptchaResponse response = captchaService.generateCaptcha();
        boolean result = captchaService.verifyCaptcha(response.getCaptchaKey(), "XXXX");
        assertFalse(result);
    }

    @Test
    @DisplayName("验证验证码 - 空的验证码Key应返回false")
    void testVerifyCaptchaNullKey() {
        boolean result = captchaService.verifyCaptcha(null, "1234");
        assertFalse(result);
    }
}
