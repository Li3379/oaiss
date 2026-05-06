package com.oaiss.chain.controller;

import com.oaiss.chain.dto.*;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器（个人中心）
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "02. 用户中心", description = "用户个人资料管理、密码修改等个人账户相关接口")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(
        summary = "获取当前用户信息", 
        description = "获取当前登录用户的完整个人资料信息，包括基本信息、企业信息（企业用户）、权限等。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = UserInfoResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "未登录或Token已过期"
        )
    })
    public ApiResponse<UserInfoResponse> getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser) {
        return ApiResponse.success(userService.getCurrentUserInfo(currentUser));
    }

    @GetMapping("/{userId}")
    @Operation(
        summary = "根据ID获取用户信息", 
        description = "根据用户ID获取用户的公开信息。仅返回可公开的用户资料。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = UserInfoResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "用户不存在"
        )
    })
    public ApiResponse<UserInfoResponse> getUserById(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        return ApiResponse.success(userService.getUserById(userId));
    }

    @PutMapping("/profile")
    @Operation(
        summary = "更新用户资料", 
        description = "更新当前用户的个人资料信息，如姓名、联系方式、头像等。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "更新成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = UserInfoResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "未登录"
        )
    })
    public ApiResponse<UserInfoResponse> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "用户资料更新请求", required = true)
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success(userService.updateProfile(currentUser, request));
    }

    @PutMapping("/password")
    @Operation(
        summary = "修改密码", 
        description = "修改当前用户的登录密码。需要验证原密码，新密码需满足安全要求。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "密码修改成功"
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
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "修改密码请求", required = true)
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(currentUser, request);
        return ApiResponse.success();
    }

    @GetMapping("/check-username")
    @Operation(
        summary = "检查用户名是否可用", 
        description = "检查指定用户名是否已被注册，用于注册时实时校验。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "检查完成，返回是否可用"
        )
    })
    public ApiResponse<Boolean> checkUsername(
            @Parameter(description = "要检查的用户名", required = true, example = "test_user")
            @RequestParam String username) {
        return ApiResponse.success(userService.isUsernameAvailable(username));
    }

    @GetMapping("/check-email")
    @Operation(
        summary = "检查邮箱是否可用", 
        description = "检查指定邮箱是否已被注册，用于注册时实时校验。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "检查完成，返回是否可用"
        )
    })
    public ApiResponse<Boolean> checkEmail(
            @Parameter(description = "要检查的邮箱", required = true, example = "test@example.com")
            @RequestParam String email) {
        return ApiResponse.success(userService.isEmailAvailable(email));
    }
}
