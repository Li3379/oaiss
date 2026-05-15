package com.oaiss.chain.service;

import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.constant.ErrorMessage;
import com.oaiss.chain.dto.PasswordChangeRequest;
import com.oaiss.chain.dto.UserInfoResponse;
import com.oaiss.chain.dto.UserProfileUpdateRequest;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.enums.UserTypeEnum;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.UserRepository;
import com.oaiss.chain.security.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务
 * 处理用户个人中心相关操作
 *
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取当前用户信息
     */
    public UserInfoResponse getCurrentUserInfo(JwtUserDetails currentUser) {
        User user = userRepository.findByIdAndDeletedFalse(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        return toUserInfoResponse(user);
    }

    /**
     * 根据用户ID获取信息
     */
    public UserInfoResponse getUserById(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        return toUserInfoResponse(user);
    }

    /**
     * 更新用户资料
     */
    @Transactional
    public UserInfoResponse updateProfile(JwtUserDetails currentUser,
            UserProfileUpdateRequest request) {

        User user = userRepository.findByIdAndDeletedFalse(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        // 检查手机号唯一性
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new BusinessException(
                        ErrorCode.PHONE_EXISTS, ErrorMessage.PHONE_EXISTS);
            }
            user.setPhone(request.getPhone());
        }

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getCompany() != null) {
            user.setCompany(request.getCompany());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        user = userRepository.save(user);
        log.info("User profile updated: {}", user.getUsername());

        return toUserInfoResponse(user);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(JwtUserDetails currentUser, PasswordChangeRequest request) {
        // 1. 校验新密码一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次密码不一致");
        }

        // 2. 查找用户
        User user = userRepository.findByIdAndDeletedFalse(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        // 3. 验证原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(
                    ErrorCode.OLD_PASSWORD_ERROR, ErrorMessage.OLD_PASSWORD_ERROR);
        }

        // 4. 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("User password changed: {}", user.getUsername());
    }

    /**
     * 检查用户名是否可用
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否可用
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    // ==================== 私有方法 ====================

    private UserInfoResponse toUserInfoResponse(User user) {
        String userTypeDesc = "";
        try {
            userTypeDesc = UserTypeEnum.fromCode(user.getUserType()).getDescription();
        } catch (Exception ignored) {
            userTypeDesc = "未知";
        }

        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .company(user.getCompany())
                .address(user.getAddress())
                .userType(user.getUserType())
                .userTypeDesc(userTypeDesc)
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
