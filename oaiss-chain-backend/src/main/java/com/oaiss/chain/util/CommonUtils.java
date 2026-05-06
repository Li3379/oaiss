package com.oaiss.chain.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 通用工具类
 * Common Utilities
 * 
 * @author OAISS Team
 */
@Slf4j
public final class CommonUtils {

    private CommonUtils() {
        // 防止实例化
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 请求相关 ====================

    /**
     * 获取当前请求
     */
    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端IP地址
     */
    public static String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 获取请求路径
     */
    public static String getRequestPath() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getRequestURI() : "unknown";
    }

    /**
     * 获取请求方法
     */
    public static String getRequestMethod() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getMethod() : "unknown";
    }

    // ==================== ID生成 ====================

    /**
     * 生成UUID（无横杠）
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成请求ID
     */
    public static String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + generateUuid().substring(0, 8);
    }

    /**
     * 生成交易ID
     */
    public static String generateTradeId() {
        return "trade_" + System.currentTimeMillis() + "_" + generateUuid().substring(0, 8);
    }

    /**
     * 生成订单ID
     */
    public static String generateOrderId() {
        return "order_" + System.currentTimeMillis() + "_" + generateUuid().substring(0, 8);
    }

    /**
     * 生成报告ID
     */
    public static String generateReportId() {
        return "report_" + System.currentTimeMillis() + "_" + generateUuid().substring(0, 8);
    }

    // ==================== 时间处理 ====================

    /**
     * 格式化日期时间
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }

    /**
     * 获取当前时间字符串
     */
    public static String getCurrentDateTimeString() {
        return formatDateTime(LocalDateTime.now());
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    // ==================== 分页参数处理 ====================

    /**
     * 分页大小上限
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 分页大小默认值
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 安全化分页大小参数
     * 将超过上限的值限制为MAX_PAGE_SIZE，无效值替换为默认值
     */
    public static int sanitizePageSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    // ==================== 字符串处理 ====================

    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 首字母小写
     */
    public static String uncapitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 首字母大写
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    // ==================== 数值处理 ====================

    /**
     * 安全解析Long
     */
    public static Long parseLong(String str, Long defaultValue) {
        try {
            return isNotEmpty(str) ? Long.parseLong(str) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全解析Integer
     */
    public static Integer parseInteger(String str, Integer defaultValue) {
        try {
            return isNotEmpty(str) ? Integer.parseInt(str) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全解析Double
     */
    public static Double parseDouble(String str, Double defaultValue) {
        try {
            return isNotEmpty(str) ? Double.parseDouble(str) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
