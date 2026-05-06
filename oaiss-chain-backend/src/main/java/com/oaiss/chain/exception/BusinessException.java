package com.oaiss.chain.exception;

import com.oaiss.chain.constant.ErrorCode;
import lombok.Getter;

/**
 * 基础业务异常
 * Base Business Exception
 * 
 * @author OAISS Team
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 系统错误
     */
    public static BusinessException systemError(String message) {
        return new BusinessException(ErrorCode.SYSTEM_ERROR, message);
    }

    /**
     * 系统错误（带异常）
     */
    public static BusinessException systemError(String message, Throwable cause) {
        return new BusinessException(ErrorCode.SYSTEM_ERROR, message, cause);
    }

    /**
     * 参数错误
     */
    public static BusinessException paramError(String message) {
        return new BusinessException(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 资源不存在
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /**
     * 认证失败
     */
    public static BusinessException authFailed(String message) {
        return new BusinessException(ErrorCode.LOGIN_FAILED, message);
    }

    /**
     * 权限不足
     */
    public static BusinessException permissionDenied(String message) {
        return new BusinessException(ErrorCode.PERMISSION_DENIED, message);
    }
}
