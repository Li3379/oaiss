# OAISS CHAIN AI智能模块技术规范 (SPEC)

**版本**: 1.0.0
**日期**: 2026-05-10
**状态**: 待实现

---

## 1. 概述

### 1.1 目的

本规范定义 OAISS CHAIN 系统AI智能模块的技术实现要求，包括碳排放预测、企业境况推断等核心功能。

### 1.2 当前状态

- **CarbonPredictionService**: 已实现，使用简单线性回归（Stub）
- **企业境况智能推断**: 未实现
- **可信度评分**: 简单实现

---

## 2. AI预测模块规范

### 2.1 功能需求

根据原始文档（01-项目需求分析.md 3.6节）：

| 功能 | 描述 | 当前状态 |
|------|------|----------|
| 市场智能预测 | 预测未来碳交易价格、成交量、趋势 | ⚠️ 部分 |
| 企业境况推断 | 基于历史数据推断企业发展状况、潜在风险 | ❌ 未实现 |
| 可视化展示 | 图表形式展示预测结果 | ⚠️ 部分 |

### 2.2 碳排放预测服务升级

#### 2.2.1 当前实现问题

```java
// 当前Stub实现的问题：
// 1. 仅使用线性回归
// 2. 未考虑季节性因素
// 3. 无外部变量影响
// 4. 可信度计算过于简单
```

#### 2.2.2 升级方案

**方案A：Prophet时间序列预测**

```java
public interface EnhancedPredictionService {
    
    /**
     * 使用Prophet模型预测碳排放
     * @param enterpriseId 企业ID
     * @param predictMonths 预测月数
     * @return 预测结果
     */
    PredictionResult predictWithProphet(Long enterpriseId, int predictMonths);
    
    /**
     * 考虑季节性因素的预测
     */
    PredictionResult predictWithSeasonality(Long enterpriseId, 
                                             int predictMonths,
                                             SeasonalityConfig config);
}
```

**预测结果结构**：

```java
public class PredictionResult {
    /** 企业ID */
    private Long enterpriseId;
    
    /** 预测点列表 */
    private List<PredictionPoint> predictions;
    
    /** 置信区间（下界） */
    private List<BigDecimal> lowerBound;
    
    /** 置信区间（上界） */
    private List<BigDecimal> upperBound;
    
    /** 模型置信度 */
    private BigDecimal confidence;
    
    /** 季节性分解 */
    private SeasonalityDecomposition seasonality;
    
    /** 趋势分析 */
    private TrendAnalysis trend;
    
    /** 预测建议 */
    private List<Recommendation> recommendations;
    
    /** 模型类型 */
    private String modelType;
    
    /** 生成时间 */
    private LocalDateTime generatedAt;
}
```

#### 2.2.3 预测模型选择

| 场景 | 推荐模型 | 原因 |
|------|----------|------|
| 数据充足(>12期) | Prophet | 处理季节性好 |
| 数据中等(6-12期) | ARIMA | 传统时间序列 |
| 数据不足(<6期) | 线性回归 | 简单可靠 |
| 高精度要求 | LSTM深度学习 | 需大量数据 |

### 2.3 企业境况智能推断服务

#### 2.3.1 服务定义

```java
public interface EnterpriseInsightService {
    
    /**
     * 分析企业境况
     * @param enterpriseId 企业ID
     * @return 境况分析报告
     */
    EnterpriseInsight analyzeEnterprise(Long enterpriseId);
    
    /**
     * 风险预警检测
     * @param enterpriseId 企业ID
     * @return 风险评估结果
     */
    RiskAssessment assessRisk(Long enterpriseId);
    
    /**
     * 异常行为检测
     * @param enterpriseId 企业ID
     * @return 异常检测结果
     */
    AnomalyDetection detectAnomalies(Long enterpriseId);
}
```

#### 2.3.2 数据模型

```java
public class EnterpriseInsight {
    /** 企业ID */
    private Long enterpriseId;
    
    /** 综合评分（0-100） */
    private BigDecimal overallScore;
    
    /** 发展状况评估 */
    private DevelopmentStatus development;
    
    /** 风险等级 */
    private RiskLevel riskLevel;
    
    /** 潜在问题列表 */
    private List<PotentialIssue> potentialIssues;
    
    /** 改进建议 */
    private List<ImprovementSuggestion> suggestions;
    
    /** 分析依据 */
    private AnalysisEvidence evidence;
    
    /** 生成时间 */
    private LocalDateTime analyzedAt;
}

public enum RiskLevel {
    LOW,        // 低风险
    MEDIUM,     // 中等风险
    HIGH,       // 高风险
    CRITICAL    // 严重风险
}

public class PotentialIssue {
    /** 问题类型 */
    private IssueType type;  // 数据异常、排放超标、交易异常等
    
    /** 问题描述 */
    private String description;
    
    /** 严重程度 */
    private Severity severity;
    
    /** 触发条件 */
    private String triggerCondition;
    
    /** 相关证据 */
    private List<String> evidenceIds;
}
```

#### 2.3.3 推断规则

**基于原始文档（03-项目详细方案.md）要求**：

```yaml
# 企业境况推断规则配置
inference_rules:
  # 数据真实性检测
  data_authenticity:
    - rule: "连续3期报告数据波动>50%"
      risk: MEDIUM
      action: "标记为数据异常"
    
    - rule: "报告数据与行业平均水平偏差>200%"
      risk: HIGH
      action: "触发人工审核"
  
  # 违规检测
  violation_detection:
    - rule: "信誉分低于40"
      risk: HIGH
      action: "限制交易权限"
    
    - rule: "连续迟交报告3次"
      risk: MEDIUM
      action: "发送预警通知"
  
  # 发展状况评估
  development_assessment:
    - factor: "碳排放趋势"
      weight: 0.3
    - factor: "交易活跃度"
      weight: 0.2
    - factor: "信誉分变化"
      weight: 0.25
    - factor: "行业对比"
      weight: 0.25
```

### 2.4 AI可视化接口

#### 2.4.1 前端图表数据格式

```typescript
// 预测图表数据格式
interface PredictionChartData {
  // 时间轴
  periods: string[];
  
  // 历史数据
  historical: number[];
  
  // 预测数据
  predicted: number[];
  
  // 置信区间
  confidenceInterval: {
    lower: number[];
    upper: number[];
  };
  
  // 关键节点标注
  annotations: Annotation[];
}

// 企业境况雷达图数据
interface InsightRadarData {
  dimensions: string[];  // ["碳排放管理", "交易合规", "信誉评分", "数据质量", "发展潜力"]
  values: number[];      // 各维度得分 0-100
  benchmarks: number[];  // 行业平均对比
}
```

---

## 3. AI模块API规范

### 3.1 碳排放预测接口

```
POST /api/v1/ai/prediction/carbon
Content-Type: application/json

Request:
{
    "enterpriseId": 123,
    "predictMonths": 6,
    "modelType": "AUTO",  // AUTO, PROPHET, ARIMA, LINEAR
    "includeSeasonality": true,
    "confidenceLevel": 0.95
}

Response:
{
    "code": 200,
    "data": {
        "predictions": [
            {"period": "2026-06", "value": 1234.56, "lower": 1100.0, "upper": 1400.0},
            {"period": "2026-07", "value": 1245.67, "lower": 1080.0, "upper": 1420.0}
        ],
        "confidence": 0.85,
        "modelUsed": "PROPHET",
        "trend": "INCREASING",
        "recommendations": [
            "预计下月排放量上升，建议加强节能措施",
            "当前排放趋势符合行业平均水平"
        ]
    }
}
```

### 3.2 企业境况分析接口

```
POST /api/v1/ai/insight/analyze
Content-Type: application/json

Request:
{
    "enterpriseId": 123,
    "analysisDepth": "FULL"  // BASIC, STANDARD, FULL
}

Response:
{
    "code": 200,
    "data": {
        "overallScore": 78.5,
        "development": {
            "trend": "STABLE",
            "growthRate": 0.05,
            "industryPosition": "ABOVE_AVERAGE"
        },
        "riskLevel": "LOW",
        "potentialIssues": [],
        "suggestions": [
            {
                "category": "EMISSION_MANAGEMENT",
                "priority": "MEDIUM",
                "description": "建议优化能源结构，降低碳排放强度"
            }
        ],
        "radarData": {
            "dimensions": ["碳排放管理", "交易合规", "信誉评分", "数据质量", "发展潜力"],
            "values": [75, 85, 80, 70, 82],
            "benchmarks": [70, 75, 75, 70, 70]
        }
    }
}
```

### 3.3 风险预警接口

```
GET /api/v1/ai/risk/alerts?enterpriseId=123

Response:
{
    "code": 200,
    "data": {
        "alerts": [
            {
                "alertId": "ALT-001",
                "type": "DATA_ANOMALY",
                "severity": "WARNING",
                "message": "近3期报告数据波动较大",
                "triggeredAt": "2026-05-10T10:00:00",
                "recommendedAction": "建议人工复核"
            }
        ],
        "summary": {
            "total": 1,
            "critical": 0,
            "warning": 1,
            "info": 0
        }
    }
}
```

---

## 4. 实现计划

### 4.1 阶段划分

| 阶段 | 任务 | 工时 | 优先级 |
|------|------|------|--------|
| 1 | 升级 CarbonPredictionService，集成Prophet | 3天 | P1 |
| 2 | 实现 EnterpriseInsightService | 5天 | P1 |
| 3 | 实现风险预警模块 | 3天 | P1 |
| 4 | 前端可视化升级 | 4天 | P2 |
| 5 | 模型训练与调优（可选） | 5天 | P2 |

### 4.2 技术选型

| 组件 | 推荐方案 | 备选方案 |
|------|----------|----------|
| 时间序列预测 | Prophet (Python服务) | statsmodels (Java) |
| 异常检测 | Isolation Forest | 统计方法 |
| 规则引擎 | Drools | 自定义规则服务 |
| 模型服务 | Python Flask API | Spring Boot + DJL |

---

## 5. 测试验收标准

### 5.1 预测准确性

- 短期预测（1-3月）误差 < 15%
- 中期预测（4-6月）误差 < 25%
- 置信区间覆盖率 > 90%

### 5.2 境况推断准确性

- 风险预警准确率 > 80%
- 异常检测误报率 < 10%
- 建议相关性评分 > 70%

### 5.3 性能要求

- 单次预测响应时间 < 3秒
- 批量分析吞吐量 > 100企业/分钟
