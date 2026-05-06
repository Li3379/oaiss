package com.oaiss.chain.controller;

import com.oaiss.chain.dto.*;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理登录、注册、验证码、Token刷新等
 * 
 * @author OAISS Team
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "01. 认证管理", description = "用户登录、注册、验证码、Token刷新等认证相关接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "用户登录", 
        description = "使用用户名密码进行登录，支持图形验证码校验。登录成功返回JWT令牌。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "登录成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = LoginResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "用户名或密码错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "1005", 
            description = "验证码错误或已过期"
        )
    })
    public ApiResponse<LoginResponse> login(
            @Parameter(description = "登录请求参数", required = true)
            @Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(
        summary = "用户注册", 
        description = "注册新用户账号。支持企业用户、审核员、第三方机构、管理员等不同类型用户注册。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "注册成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = LoginResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "1001", 
            description = "用户名已存在"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "1002", 
            description = "邮箱已被注册"
        )
    })
    public ApiResponse<LoginResponse> register(
            @Parameter(description = "注册请求参数", required = true)
            @Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "注册成功");
    }

    @PostMapping("/captcha")
    @Operation(
        summary = "获取图形验证码", 
        description = "生成图形验证码，返回验证码图片Base64和验证码Key。验证码有效期5分钟。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = CaptchaResponse.class))
        )
    })
    public ApiResponse<CaptchaResponse> getCaptcha() {
        return ApiResponse.success(authService.generateCaptcha());
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "刷新访问令牌", 
        description = "使用RefreshToken获取新的AccessToken，延长用户会话有效期。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "刷新成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = LoginResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "RefreshToken无效或已过期"
        )
    })
    public ApiResponse<LoginResponse> refreshToken(
            @Parameter(description = "刷新令牌", required = true)
            @RequestHeader("Refresh-Token") String refreshToken) {
        return ApiResponse.success(authService.refreshToken(refreshToken));
    }

    @GetMapping("/check-ip")
    @Operation(
        summary = "IP访问检查", 
        description = "检查当前IP地址是否在允许访问的白名单中。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "检查成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "1003", 
            description = "IP不在白名单中"
        )
    })
    public ApiResponse<Boolean> checkIp() {
        return ApiResponse.success(true);
    }

    @GetMapping("/me")
    @Operation(
        summary = "获取当前用户信息", 
        description = "获取当前已登录用户的详细信息，包括用户ID、用户名、用户类型等。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = JwtUserDetails.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "未登录或Token已过期"
        )
    })
    public ApiResponse<JwtUserDetails> getCurrentUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal JwtUserDetails userDetails) {
        return ApiResponse.success(userDetails);
    }

    @PostMapping("/logout")
    @Operation(
        summary = "用户登出", 
        description = "用户退出登录，清除服务端Token缓存。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "登出成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "未登录"
        )
    })
    public ApiResponse<Void> logout(
            @Parameter(hidden = true)
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Parameter(hidden = true)
            HttpServletRequest request) {
        // 提取当前Token
        String bearerToken = request.getHeader("Authorization");
        String token = (bearerToken != null && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7) : null;
        authService.logout(userDetails.getUsername(), token);
        return ApiResponse.success(null, "登出成功");
    }

    @PutMapping("/password")
    @Operation(
        summary = "修改密码", 
        description = "用户修改自己的登录密码，需要验证原密码。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "修改成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "原密码错误"
        )
    })
    public ApiResponse<Void> changePassword(
            @Parameter(description = "修改密码请求参数", required = true)
            @Valid @RequestBody PasswordChangeRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal JwtUserDetails userDetails) {
        authService.changePassword(userDetails.getUsername(), request);
        return ApiResponse.success(null, "密码修改成功");
    }
}
