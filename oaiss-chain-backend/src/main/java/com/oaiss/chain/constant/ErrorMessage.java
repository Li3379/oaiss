package com.oaiss.chain.constant;

/**
 * 错误消息常量定义
 * Error Message Constants
 * 
 * @author OAISS Team
 */
public final class ErrorMessage {

    private ErrorMessage() {
        // 防止实例化
    }

    // ==================== 公共错误消息 ====================
    
    public static final String SYSTEM_ERROR = "系统内部错误，请稍后重试";
    public static final String PARAM_ERROR = "请求参数错误";
    public static final String RESOURCE_NOT_FOUND = "请求的资源不存在";
    public static final String METHOD_NOT_SUPPORTED = "不支持的请求方法";
    public static final String REQUEST_TIMEOUT = "请求超时";
    public static final String SERVICE_UNAVAILABLE = "服务暂时不可用";
    public static final String DATABASE_ERROR = "数据库操作失败";
    public static final String FILE_UPLOAD_ERROR = "文件上传失败";
    public static final String FILE_SIZE_EXCEEDED = "文件大小超过限制";
    public static final String FILE_TYPE_NOT_SUPPORTED = "不支持的文件类型";

    // ==================== 认证授权错误消息 ====================
    
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String LOGIN_FAILED = "用户名或密码错误";
    public static final String TOKEN_INVALID = "Token无效";
    public static final String TOKEN_EXPIRED = "Token已过期，请重新登录";
    public static final String PERMISSION_DENIED = "无权限访问该资源";
    public static final String ACCOUNT_DISABLED = "账号已被禁用";
    public static final String CAPTCHA_ERROR = "验证码错误";
    public static final String CAPTCHA_EXPIRED = "验证码已过期";
    public static final String IP_VALIDATION_FAILED = "IP地址验证失败";
    public static final String ACCOUNT_EXISTS = "账号已存在";
    public static final String PHONE_EXISTS = "手机号已注册";
    public static final String PASSWORD_WEAK = "密码强度不足";
    public static final String OLD_PASSWORD_ERROR = "原密码错误";

    // ==================== 碳核算模块错误消息 ====================
    
    public static final String CARBON_REPORT_NOT_FOUND = "碳报告不存在";
    public static final String CARBON_DATA_SUBMIT_FAILED = "碳数据提交失败";
    public static final String CARBON_CALCULATION_FAILED = "碳核算计算失败";
    public static final String DATA_FORMAT_ERROR = "数据格式错误";
    public static final String DATA_MISSING = "必要数据缺失";
    public static final String DATA_OUT_OF_RANGE = "数据超出有效范围";
    public static final String REPORT_ALREADY_SUBMITTED = "报告已提交，无法修改";
    public static final String REPORT_ALREADY_REVIEWED = "报告已审核，无法修改";
    public static final String SIGNATURE_VERIFICATION_FAILED = "RSA签名验证失败";
    public static final String EMISSION_FACTOR_NOT_FOUND = "排放因子不存在";
    public static final String INVALID_ACCOUNTING_PERIOD = "核算周期无效";

    // ==================== 碳交易模块错误消息 ====================
    
    public static final String TRADE_NOT_FOUND = "交易不存在";
    public static final String INSUFFICIENT_BALANCE = "余额不足";
    public static final String INSUFFICIENT_QUOTA = "碳配额不足";
    public static final String AUCTION_ENDED = "拍卖已结束";
    public static final String AUCTION_NOT_STARTED = "拍卖未开始";
    public static final String BID_TOO_LOW = "出价过低";
    public static final String ORDER_CANCELLED = "订单已取消";
    public static final String ORDER_COMPLETED = "订单已完成";
    public static final String SAME_PARTY_ERROR = "交易双方不能为同一方";
    public static final String PEER_OFFLINE = "P2P交易对方不在线";
    public static final String TRADE_AMOUNT_EXCEEDED = "交易金额超出限制";
    public static final String ORDER_ALREADY_EXISTS = "挂单已存在";

    // ==================== 区块链模块错误消息 ====================
    
    public static final String BLOCKCHAIN_CONNECTION_FAILED = "区块链连接失败";
    public static final String CHAINCODE_INVOKE_FAILED = "链码调用失败";
    public static final String TX_COMMIT_FAILED = "交易上链失败";
    public static final String BLOCK_QUERY_FAILED = "区块查询失败";
    public static final String TX_QUERY_FAILED = "交易查询失败";
    public static final String CHANNEL_NOT_FOUND = "通道不存在";
    public static final String IDENTITY_AUTH_FAILED = "身份验证失败";
    public static final String SMART_CONTRACT_ERROR = "智能合约执行错误";
}
