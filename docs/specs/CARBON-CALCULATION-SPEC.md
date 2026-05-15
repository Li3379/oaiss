# OAISS CHAIN 碳核算模型技术规范 (SPEC)

**版本**: 1.0.0
**日期**: 2026-05-10
**状态**: 待实现

---

## 1. 概述

### 1.1 目的

本规范定义 OAISS CHAIN 系统碳核算模块的技术实现要求，确保与《中国发电企业温室气体排放核算方法与报告指南（试行）》、《中国电网企业温室气体排放核算方法与报告指南（试行）》等国家标准保持一致。

### 1.2 范围

本规范涵盖：
- 发电企业碳核算模型（25参数）
- 电网企业碳核算模型（9参数）
- 数据输入接口规范
- 计算引擎接口规范
- 验证规则

---

## 2. 发电企业碳核算模型

### 2.1 计算公式

温室气体排放总量计算公式：

```
E = Σ(FCi × NCVi × 10^-6) × (CCi × OFi × 44/12) 
  + Σ(Σ(Bk,m × 90%) × (EFk,t × 100%))
  + (AD电 × EF电)
```

其中：
- E：温室气体排放总量（吨CO2当量）
- FCi：第i种化石燃料的消耗量（吨或10^3标准立方米）
- NCVi：第i种化石燃料的平均低位发热值（千焦/千克或千焦/标准立方米）
- CCi：第i种化石燃料的单位热值含碳量（吨碳/太焦）
- OFi：第i种化石燃料的碳氧化率（%）
- Bk,m：脱硫剂消耗量
- EFk,t：脱硫过程排放因子
- AD电：净购入电量
- EF电：电网排放因子

### 2.2 数据输入规范

#### 2.2.1 数据库实体扩展

```java
// CarbonReport 实体扩展字段
public class CarbonReportEmissionData {
    // === 化石燃料相关 ===
    
    /** 燃煤消耗量（吨） */
    private BigDecimal coalConsumption;
    
    /** 燃煤日平均低位热值（千焦/千克） */
    private BigDecimal coalNcvDaily;
    
    /** 燃煤日消耗量记录（JSON数组） */
    private String coalDailyConsumptionJson;
    
    /** 燃油消耗量（吨） */
    private BigDecimal oilConsumption;
    
    /** 燃油批次低位热值（JSON数组） */
    private String oilNcvBatchJson;
    
    /** 燃油批次消耗量（JSON数组） */
    private String oilBatchConsumptionJson;
    
    /** 天然气月消耗量（10^3标准立方米） */
    private BigDecimal gasConsumption;
    
    /** 天然气月平均低位热值 */
    private BigDecimal gasNcvMonthly;
    
    /** 生物质混合燃料消耗量 */
    private BigDecimal biomassConsumption;
    
    /** 生物质月平均低位热值 */
    private BigDecimal biomassNcvMonthly;
    
    // === 含碳量相关 ===
    
    /** 燃煤月平均元素碳含量（%） */
    private BigDecimal coalCarbonContent;
    
    /** 燃油单位热值含碳量（吨碳/太焦）- 默认值参考 */
    private BigDecimal oilCarbonContent;
    
    /** 燃气单位热值含碳量（吨碳/太焦）- 默认值参考 */
    private BigDecimal gasCarbonContent;
    
    // === 碳氧化率相关 ===
    
    /** 炉渣产量（吨） */
    private BigDecimal slagOutput;
    
    /** 飞灰产量（吨） */
    private BigDecimal ashOutput;
    
    /** 炉渣平均含碳量（%） */
    private BigDecimal slagCarbonContent;
    
    /** 飞灰平均含碳量（%） */
    private BigDecimal ashCarbonContent;
    
    /** 除尘系统效率（%） */
    private BigDecimal dustRemovalEfficiency;
    
    /** 燃煤碳氧化率 - 可计算或使用默认值98% */
    private BigDecimal coalOxidationRate;
    
    /** 燃油碳氧化率 - 默认值98% */
    private BigDecimal oilOxidationRate;
    
    /** 燃气碳氧化率 - 默认值99% */
    private BigDecimal gasOxidationRate;
    
    // === 脱硫相关 ===
    
    /** 脱硫剂月消耗量（JSON数组） */
    private String desulfurizerConsumptionJson;
    
    /** 脱硫排放因子（吨CO2/吨） */
    private BigDecimal desulfurizerEmissionFactor;
    
    // === 电力相关 ===
    
    /** 净购入电量（兆瓦时） */
    private BigDecimal purchasedElectricity;
    
    /** 电网排放因子（吨CO2/兆瓦时） */
    private BigDecimal gridEmissionFactor;
    
    // === 计算结果 ===
    
    /** 燃料燃烧排放量 */
    private BigDecimal fuelCombustionEmission;
    
    /** 脱硫过程排放量 */
    private BigDecimal desulfurizationEmission;
    
    /** 净购入电力排放量 */
    private BigDecimal electricityEmission;
    
    /** 总排放量 */
    private BigDecimal totalEmission;
}
```

#### 2.2.2 输入参数默认值

根据表1《常用化石燃料相关参数缺省值》：

| 能源名称 | 平均低位发热值(kJ/kg) | 单位热值含碳量(tC/TJ) | 碳氧化率(%) |
|----------|---------------------|----------------------|-------------|
| 原油 | 41816 | 20.08 | 98 |
| 燃料油 | 41816 | 21.1 | 98 |
| 汽油 | 43070 | 18.9 | 98 |
| 柴油 | 42652 | 20.2 | 98 |
| 天然气 | 38931 | 15.32 | 99 |
| 焦炉煤气 | 12726~17981 | 13.58 | - |

### 2.3 计算服务接口

```java
public interface CarbonCalculationService {
    
    /**
     * 计算发电企业碳排放量
     * @param data 原始输入数据
     * @return 计算结果
     */
    CarbonCalculationResult calculatePowerGeneration(CarbonReportEmissionData data);
    
    /**
     * 验证输入数据完整性
     * @param data 原始输入数据
     * @return 验证结果
     */
    ValidationResult validateInputData(CarbonReportEmissionData data);
    
    /**
     * 获取默认参数值
     * @param fuelType 燃料类型
     * @return 默认参数
     */
    FuelDefaultParameters getDefaultParameters(String fuelType);
}
```

---

## 3. 电网企业碳核算模型

### 3.1 计算公式

温室气体排放总量计算公式：

```
E = (Σ(REC容量,i - REC回收,i) + Σ(REP容量,j - REP回收,j)) × GWP_SF6 × 10^-3
  + (EL上网 + EL输入 - EL输出 - EL售电) × EF电网
```

其中：
- REC容量,i：退役设备i的SF6容量（千克）
- REC回收,i：退役设备i的SF6实际回收量（千克）
- REP容量,j：修理设备j的SF6容量（千克）
- REP回收,j：修理设备j的SF6实际回收量（千克）
- GWP_SF6：SF6温室气体潜能，定值23900
- EL上网：电厂上网电量
- EL输入：自外省输入电量
- EL输出：向外省输出电量
- EL售电：售电量
- EF电网：区域电网平均供电排放因子

### 3.2 数据输入规范

```java
public class GridEmissionData {
    // === SF6相关 ===
    
    /** 退役设备SF6容量记录（JSON数组） */
    private String retiredEquipmentCapacityJson;
    
    /** 退役设备SF6回收量记录（JSON数组） */
    private String retiredEquipmentRecoveryJson;
    
    /** 修理设备SF6容量记录（JSON数组） */
    private String repairEquipmentCapacityJson;
    
    /** 修理设备SF6回收量记录（JSON数组） */
    private String repairEquipmentRecoveryJson;
    
    // === 电力相关 ===
    
    /** 电厂上网电量（兆瓦时） */
    private BigDecimal generationOutput;
    
    /** 自外省输入电量（兆瓦时） */
    private BigDecimal importedElectricity;
    
    /** 向外省输出电量（兆瓦时） */
    private BigDecimal exportedElectricity;
    
    /** 售电量（兆瓦时） */
    private BigDecimal soldElectricity;
    
    /** 电网排放因子（吨CO2/兆瓦时） */
    private BigDecimal gridEmissionFactor;
    
    // === 计算结果 ===
    
    /** SF6泄漏排放量 */
    private BigDecimal sf6Emission;
    
    /** 电力传输排放量 */
    private BigDecimal transmissionEmission;
    
    /** 总排放量 */
    private BigDecimal totalEmission;
}
```

---

## 4. API 接口规范

### 4.1 碳核算计算接口

```
POST /api/v1/carbon/calculate
Content-Type: application/json

Request:
{
    "enterpriseId": 123,
    "reportPeriod": "2025",
    "industryType": "POWER_GENERATION",  // 或 "POWER_GRID"
    "emissionData": {
        // 发电企业数据 或 电网企业数据
    }
}

Response:
{
    "code": 200,
    "data": {
        "reportId": "RPT-2025-001",
        "fuelCombustionEmission": 12345.67,
        "desulfurizationEmission": 123.45,
        "electricityEmission": 456.78,
        "totalEmission": 12925.90,
        "confidence": 0.95,
        "warnings": ["部分参数使用默认值"]
    }
}
```

### 4.2 参数验证接口

```
POST /api/v1/carbon/validate
Content-Type: application/json

Request:
{
    "industryType": "POWER_GENERATION",
    "emissionData": { ... }
}

Response:
{
    "code": 200,
    "data": {
        "valid": true,
        "missingFields": ["coalNcvDaily"],
        "warnings": [
            "燃煤碳氧化率使用默认值98%"
        ]
    }
}
```

---

## 5. 实现优先级

| 优先级 | 任务 | 预估工时 |
|--------|------|----------|
| P0-1 | 扩展 CarbonReport 实体，增加原始数据字段 | 2天 |
| P0-2 | 实现 PowerGenerationCalculationService | 3天 |
| P0-3 | 实现 PowerGridCalculationService | 2天 |
| P0-4 | 单元测试（覆盖边界情况） | 2天 |
| P1-1 | 前端原始数据输入表单 | 3天 |
| P1-2 | 参数默认值配置管理 | 1天 |

---

## 6. 测试验收标准

### 6.1 单元测试

- 计算结果精度：小数点后4位
- 边界值测试：消耗量为0、极大值
- 默认值测试：缺失参数时使用默认值

### 6.2 集成测试

- 完整流程：输入 → 计算 → 存储 → 审核
- 与国家标准计算器对比验证

### 6.3 验收标准

- 与国家标准计算公式结果误差 < 0.01%
- 所有必填字段有验证提示
- 计算过程可追溯
