package com.oaiss.chain.service;

import com.oaiss.chain.dto.CarbonPredictionRequest;
import com.oaiss.chain.dto.CarbonPredictionResponse;
import com.oaiss.chain.entity.CarbonReport;
import com.oaiss.chain.repository.CarbonReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI碳排放预测服务
 * 基于历史碳报告数据进行简单趋势预测
 * <p>
 * 设计文档要求（doc03）：
 * - AI驱动的碳排放趋势预测
 * - 预测可信度评分
 * <p>
 * 当前为Stub实现，生产环境替换为ML模型
 *
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonPredictionService {

    private final CarbonReportRepository carbonReportRepository;

    /**
     * 预测企业未来碳排放趋势
     * 使用线性回归Stub（基于最近N期报告的趋势外推）
     */
    public CarbonPredictionResponse predict(CarbonPredictionRequest request) {
        // 获取企业历史报告（最近12期）
        List<CarbonReport> reports = carbonReportRepository
                .findByEnterpriseIdAndDeletedFalse(request.getEnterpriseId(), PageRequest.of(0, 12))
                .getContent();

        if (reports.size() < 2) {
            return CarbonPredictionResponse.builder()
                    .enterpriseId(request.getEnterpriseId())
                    .confidence(0.1)
                    .message("历史数据不足，至少需要2期碳报告")
                    .predictions(List.of())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // 计算平均变化率
        BigDecimal totalChange = BigDecimal.ZERO;
        int changes = 0;
        for (int i = 1; i < reports.size(); i++) {
            BigDecimal prev = reports.get(i - 1).getTotalEmission();
            BigDecimal curr = reports.get(i).getTotalEmission();
            if (prev != null && curr != null && prev.compareTo(BigDecimal.ZERO) > 0) {
                totalChange = totalChange.add(curr.subtract(prev));
                changes++;
            }
        }

        BigDecimal avgChange = changes > 0
                ? totalChange.divide(BigDecimal.valueOf(changes), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 生成预测
        BigDecimal latestEmission = reports.get(0).getTotalEmission();
        int predictMonths = request.getPredictMonths() != null ? request.getPredictMonths() : 6;
        List<CarbonPredictionResponse.PredictionPoint> predictions = new ArrayList<>();

        for (int i = 1; i <= predictMonths; i++) {
            BigDecimal predicted = latestEmission.add(avgChange.multiply(BigDecimal.valueOf(i)));
            predictions.add(CarbonPredictionResponse.PredictionPoint.builder()
                    .period("M+" + i)
                    .predictedEmission(predicted.max(BigDecimal.ZERO))
                    .build());
        }

        // 可信度：数据越多越可信，最大0.85（Stub模型上限）
        double confidence = Math.min(0.85, 0.3 + reports.size() * 0.05);

        return CarbonPredictionResponse.builder()
                .enterpriseId(request.getEnterpriseId())
                .confidence(confidence)
                .message("Stub预测模型 - 生产环境替换为ML模型")
                .predictions(predictions)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
