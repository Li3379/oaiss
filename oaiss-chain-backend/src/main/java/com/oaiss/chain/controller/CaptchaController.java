package com.oaiss.chain.controller;

import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.CaptchaResponse;
import com.oaiss.chain.dto.CaptchaSendRequest;
import com.oaiss.chain.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 验证码控制器
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
@Tag(name = "06. 验证码管理", description = "图形验证码生成与验证接口")
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping("/generate")
    @Operation(
        summary = "生成图形验证码", 
        description = "生成图形验证码，返回验证码图片的Base64编码和验证码Key。验证码有效期为5分钟。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "生成成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = CaptchaResponse.class))
        )
    })
    public ApiResponse<CaptchaResponse> generate() {
        return ApiResponse.success(captchaService.generateCaptcha());
    }

    @PostMapping("/verify")
    @Operation(
        summary = "验证验证码", 
        description = "校验用户输入的验证码是否正确。验证后验证码立即失效。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "验证完成，返回是否正确"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误"
        )
    })
    public ApiResponse<Boolean> verify(
            @Parameter(description = "验证请求，包含captchaKey和captchaCode", required = true)
            @RequestBody Map<String, String> body) {
        String key = body.get("captchaKey");
        String code = body.get("captchaCode");
        return ApiResponse.success(captchaService.verifyCaptcha(key, code));
    }

    @PostMapping("/sms/send")
    @Operation(
        summary = "发送短信验证码", 
        description = "向指定手机号发送短信验证码。用于手机验证、找回密码等场景。验证码有效期为5分钟。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "发送成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "手机号格式错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429", 
            description = "发送过于频繁，请稍后再试"
        )
    })
    public ApiResponse<Void> sendSmsCode(
            @Parameter(description = "发送短信验证码请求", required = true)
            @Valid @RequestBody CaptchaSendRequest request) {
        captchaService.sendSmsCode(request.getTarget(), request.getType());
        return ApiResponse.success(null, "验证码已发送");
    }

    @PostMapping("/email/send")
    @Operation(
        summary = "发送邮箱验证码", 
        description = "向指定邮箱发送验证码。用于邮箱验证、找回密码等场景。验证码有效期为10分钟。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "发送成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "邮箱格式错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429", 
            description = "发送过于频繁"
        )
    })
    public ApiResponse<Void> sendEmailCode(
            @Parameter(description = "发送邮箱验证码请求", required = true)
            @Valid @RequestBody CaptchaSendRequest request) {
        captchaService.sendEmailCode(request.getTarget(), request.getType());
        return ApiResponse.success(null, "验证码已发送至邮箱");
    }
}
