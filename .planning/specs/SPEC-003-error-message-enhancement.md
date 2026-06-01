---
status: draft
priority: medium
created: 2026-05-10
author: claude
related:
  - Phase 2 UAT findings
  - User experience
---

# SPEC: 错误信息国际化与友好化

## 问题描述

当前错误信息存在以下问题：
1. 错误信息过于技术化，用户难以理解
2. 缺少上下文信息（如具体是哪个企业、哪个年份）
3. 未国际化，仅支持中文

## 当前状态

```java
// 当前错误信息
throw new BusinessException(ErrorCode.EMISSION_RATING_ALREADY_EXISTS);
// 错误码定义
EMISSION_RATING_ALREADY_EXISTS(3001, "该企业2025年评级已存在")
```

**问题**：
- 用户不知道是哪个企业
- 用户不知道是哪个年份
- 年份硬编码在错误信息中

## 解决方案分析

### 方案A: 动态错误信息（推荐）

**优点**：
- 信息完整
- 用户友好

**实施**：
```java
// 使用String.format动态构建错误信息
throw new BusinessException(
    ErrorCode.EMISSION_RATING_ALREADY_EXISTS,
    String.format("企业[%s]的%s年评级已存在(等级:%s)，无法重复创建",
        enterprise.getEnterpriseName(),
        ratingYear,
        existingRating.get().getRatingLevel())
);
```

### 方案B: 错误详情对象

**优点**：
- 结构化
- 前端可灵活展示

**实施**：
```java
// 返回详细错误对象
ApiResponse.error(ErrorCode.EMISSION_RATING_ALREADY_EXISTS)
    .withDetail("enterpriseName", enterprise.getEnterpriseName())
    .withDetail("ratingYear", ratingYear)
    .withDetail("existingLevel", existingRating.get().getRatingLevel());
```

### 方案C: 国际化错误信息

**优点**：
- 多语言支持
- 标准化

**实施**：
```java
// 使用MessageSource
@MessageSource
private MessageSource messageSource;

String message = messageSource.getMessage(
    "error.emission.rating.exists",
    new Object[]{enterpriseName, ratingYear, existingLevel},
    LocaleContextHolder.getLocale()
);
```

## 推荐方案

**采用方案A + 方案C组合**：

1. 动态构建错误信息，包含上下文
2. 支持国际化
3. 保持现有ApiResponse结构

## 实施细节

### 1. 扩展ErrorCode枚举

```java
// ErrorCode.java
public enum ErrorCode {
    // ... 现有错误码 ...

    // 修改：使用占位符
    EMISSION_RATING_ALREADY_EXISTS(3001, "error.emission.rating.exists"),

    // 新增错误码
    CARBON_REPORT_ALREADY_ON_CHAIN(3006, "error.carbon.report.on_chain"),
    CARBON_REPORT_DRAFT_CANNOT_REVIEW(3001, "error.carbon.report.draft.review"),
    ;

    private final int code;
    private final String messageKey;  // 改为messageKey

    // ... getter方法 ...
}
```

### 2. 创建国际化资源文件

```properties
# messages/messages_zh_CN.properties
error.emission.rating.exists=企业[{0}]的{1}年评级已存在(等级:{2})，无法重复创建
error.carbon.report.on_chain=报告[{0}]已上链，无法重复提交
error.carbon.report.draft.review=报告[{0}]为草稿状态，无法审核

# messages/messages_en_US.properties
error.emission.rating.exists=Enterprise [{0}] already has a rating for {1} (Level: {2}), cannot create duplicate
error.carbon.report.on_chain=Report [{0}] is already on-chain, cannot resubmit
error.carbon.report.draft.review=Report [{0}] is in DRAFT status, cannot review
```

### 3. 创建错误信息构建工具类

```java
// util/MessageUtils.java
@Component
public class MessageUtils {

    private static MessageSource messageSource;

    @Autowired
    public void setMessageSource(MessageSource messageSource) {
        MessageUtils.messageSource = messageSource;
    }

    /**
     * 获取国际化消息
     * @param messageKey 消息键
     * @param args 参数
     * @return 格式化后的消息
     */
    public static String getMessage(String messageKey, Object... args) {
        try {
            return messageSource.getMessage(
                messageKey,
                args,
                LocaleContextHolder.getLocale()
            );
        } catch (NoSuchMessageException e) {
            // 降级：返回键名
            return messageKey + (args.length > 0 ? Arrays.toString(args) : "");
        }
    }

    /**
     * 获取默认语言消息（中文）
     */
    public static String getMessageZh(String messageKey, Object... args) {
        try {
            return messageSource.getMessage(
                messageKey,
                args,
                Locale.SIMPLIFIED_CHINESE
            );
        } catch (NoSuchMessageException e) {
            return messageKey;
        }
    }
}
```

### 4. 修改BusinessException

```java
// exception/BusinessException.java
public class BusinessException extends RuntimeException {

    private final int code;
    private final String message;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode, Object... args) {
        this.code = errorCode.getCode();
        this.message = MessageUtils.getMessage(errorCode.getMessageKey(), args);
        this.details = new HashMap<>();
    }

    public BusinessException withDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    // getters...
}
```

### 5. 修改业务代码

```java
// CarbonReportService.java

// 修改前
if (existingRating.isPresent()) {
    throw new BusinessException(ErrorCode.EMISSION_RATING_ALREADY_EXISTS);
}

// 修改后
if (existingRating.isPresent()) {
    EmissionRating rating = existingRating.get();
    Enterprise enterprise = enterpriseRepository.findById(report.getEnterpriseId())
        .orElseThrow(() -> new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND));

    throw new BusinessException(
        ErrorCode.EMISSION_RATING_ALREADY_EXISTS,
        enterprise.getEnterpriseName(),
        rating.getRatingYear(),
        rating.getRatingLevel()
    ).withDetail("enterpriseId", report.getEnterpriseId())
     .withDetail("ratingYear", rating.getRatingYear())
     .withDetail("existingRatingId", rating.getId());
}
```

### 6. 统一ApiResponse错误响应

```java
// ApiResponse.java
@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private Map<String, Object> details;  // 新增：错误详情
    private Meta meta;

    public static <T> ApiResponse<T> error(BusinessException e) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(e.getCode());
        response.setMessage(e.getMessage());
        response.setDetails(e.getDetails());
        return response;
    }
}
```

### 7. 前端错误展示优化

```typescript
// api/error-handler.ts
export function handleApiError(error: ApiError): string {
  const { code, message, details } = error.response?.data || {};

  // 根据错误码定制展示
  switch (code) {
    case 3001:  // EMISSION_RATING_ALREADY_EXISTS
      ElMessage.error({
        message: message,
        duration: 5000,
        showClose: true,
      });
      // 可选：提供跳转到现有评级的链接
      if (details?.existingRatingId) {
        console.log(`Existing rating ID: ${details.existingRatingId}`);
      }
      break;
    default:
      ElMessage.error(message || '操作失败，请稍后重试');
  }

  return message;
}
```

## 需要修改的错误信息清单

| 错误码 | 当前信息 | 优化后 |
|--------|----------|--------|
| 3001 | 该企业2025年评级已存在 | 企业[{0}]的{1}年评级已存在(等级:{2}) |
| 3006 | 报告已上链，无法重复提交 | 报告[{0}]已上链(交易哈希:{1})，无法重复提交 |
| 3001 | 报告为草稿状态，无法审核 | 报告[{0}]为草稿状态，请先提交后再审核 |
| 3002 | 报告不存在 | 报告[{0}]不存在或已被删除 |
| 3003 | 无权限操作此报告 | 无权限操作报告[{0}]，请联系管理员 |

## 验证清单

- [ ] ErrorCode枚举更新
- [ ] 国际化资源文件创建
- [ ] MessageUtils工具类创建
- [ ] BusinessException修改
- [ ] 业务代码更新
- [ ] 前端错误处理更新
- [ ] 单元测试覆盖

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 国际化资源缺失 | 中 | 提供降级机制，返回key |
| 参数不匹配 | 低 | 编译时检查，单元测试 |
| 性能影响 | 低 | MessageSource有缓存 |

## 回滚方案

如果出现问题：
1. 恢复ErrorCode为硬编码消息
2. 移除MessageUtils依赖
3. 恢复原有BusinessException
