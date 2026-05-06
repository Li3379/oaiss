package com.oaiss.chain.service;

import com.oaiss.chain.dto.CarbonPredictionRequest;
import com.oaiss.chain.dto.CarbonPredictionResponse;
import com.oaiss.chain.entity.CarbonReport;
import com.oaiss.chain.repository.CarbonReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * AI碳排放预测服务测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CarbonPredictionServiceTest {

    @Mock
    private CarbonReportRepository carbonReportRepository;

    @InjectMocks
    private CarbonPredictionService carbonPredictionService;

    private CarbonPredictionRequest request;

    @BeforeEach
    void setUp() {
        request = new CarbonPredictionRequest();
        request.setEnterpriseId(1L);
        request.setPredictMonths(6);
    }

    @Test
    @DisplayName("预测 - 历史数据不足返回低置信度")
    void predict_InsufficientData_ShouldReturnLowConfidence() {
        when(carbonReportRepository.findByEnterpriseIdAndDeletedFalse(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(buildReport(BigDecimal.valueOf(1000)))));

        CarbonPredictionResponse response = carbonPredictionService.predict(request);

        assertNotNull(response);
        assertEquals(0.1, response.getConfidence());
        assertEquals("历史数据不足，至少需要2期碳报告", response.getMessage());
        assertTrue(response.getPredictions().isEmpty());
    }

    @Test
    @DisplayName("预测 - 无数据返回低置信度")
    void predict_NoData_ShouldReturnLowConfidence() {
        when(carbonReportRepository.findByEnterpriseIdAndDeletedFalse(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        CarbonPredictionResponse response = carbonPredictionService.predict(request);

        assertEquals(0.1, response.getConfidence());
        assertTrue(response.getPredictions().isEmpty());
    }

    @Test
    @DisplayName("预测 - 有足够数据返回预测结果")
    void predict_WithEnoughData_ShouldReturnPredictions() {
        List<CarbonReport> reports = new ArrayList<>();
        reports.add(buildReport(BigDecimal.valueOf(1000)));
        reports.add(buildReport(BigDecimal.valueOf(900)));
        reports.add(buildReport(BigDecimal.valueOf(800)));

        when(carbonReportRepository.findByEnterpriseIdAndDeletedFalse(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(reports));

        CarbonPredictionResponse response = carbonPredictionService.predict(request);

        assertNotNull(response);
        assertTrue(response.getConfidence() > 0.1);
        assertEquals(6, response.getPredictions().size());
        assertEquals("M+1", response.getPredictions().get(0).getPeriod());
        assertNotNull(response.getGeneratedAt());
    }

    @Test
    @DisplayName("预测 - 自定义预测月数")
    void predict_CustomMonths_ShouldReturnCorrectCount() {
        request.setPredictMonths(3);
        List<CarbonReport> reports = List.of(buildReport(BigDecimal.valueOf(1000)), buildReport(BigDecimal.valueOf(900)));

        when(carbonReportRepository.findByEnterpriseIdAndDeletedFalse(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(reports));

        CarbonPredictionResponse response = carbonPredictionService.predict(request);

        assertEquals(3, response.getPredictions().size());
        assertEquals("M+3", response.getPredictions().get(2).getPeriod());
    }

    @Test
    @DisplayName("预测 - null预测月数默认6个月")
    void predict_NullMonths_DefaultsTo6() {
        request.setPredictMonths(null);
        List<CarbonReport> reports = List.of(buildReport(BigDecimal.valueOf(1000)), buildReport(BigDecimal.valueOf(900)));

        when(carbonReportRepository.findByEnterpriseIdAndDeletedFalse(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(reports));

        CarbonPredictionResponse response = carbonPredictionService.predict(request);

        assertEquals(6, response.getPredictions().size());
    }

    @Test
    @DisplayName("预测 - 排放量递减趋势时预测值递减")
    void predict_DecreasingTrend_ShouldPredictDecrease() {
        // Reports newest first: 300, 200, 100
        // Changes: report[1]-report[0]=200-300=-100, report[2]-report[1]=100-200=-100
        // avgChange = -100, latestEmission = 300
        // M+1 = 300+(-100)=200, M+2 = 300+(-200)=100 → predictions decrease
        List<CarbonReport> reports = List.of(
                buildReport(BigDecimal.valueOf(300)),
                buildReport(BigDecimal.valueOf(200)),
                buildReport(BigDecimal.valueOf(100))
        );

        when(carbonReportRepository.findByEnterpriseIdAndDeletedFalse(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(reports));

        CarbonPredictionResponse response = carbonPredictionService.predict(request);

        BigDecimal m1 = response.getPredictions().get(0).getPredictedEmission();
        BigDecimal m2 = response.getPredictions().get(1).getPredictedEmission();
        assertTrue(m2.compareTo(m1) < 0, "预测值应该递减 (历史下降趋势)");
        assertTrue(m1.compareTo(new BigDecimal("200")) == 0, "M+1应为200");
        assertTrue(m2.compareTo(new BigDecimal("100")) == 0, "M+2应为100");
    }

    @Test
    @DisplayName("预测 - 报告有null排放量时跳过")
    void predict_NullEmissions_ShouldSkip() {
        List<CarbonReport> reports = new ArrayList<>();
        reports.add(buildReport(BigDecimal.valueOf(1000)));
        reports.add(buildReport(null));
        reports.add(buildReport(BigDecimal.valueOf(800)));

        when(carbonReportRepository.findByEnterpriseIdAndDeletedFalse(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(reports));

        CarbonPredictionResponse response = carbonPredictionService.predict(request);

        // Should still produce predictions with available data
        assertFalse(response.getPredictions().isEmpty());
    }

    @Test
    @DisplayName("预测 - 12份报告置信度不超过0.85")
    void predict_12Reports_ConfidenceCappedAt085() {
        List<CarbonReport> reports = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            reports.add(buildReport(BigDecimal.valueOf(1000 + i * 100)));
        }

        when(carbonReportRepository.findByEnterpriseIdAndDeletedFalse(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(reports));

        CarbonPredictionResponse response = carbonPredictionService.predict(request);

        assertTrue(response.getConfidence() <= 0.85);
        assertTrue(response.getConfidence() > 0.3);
    }

    private CarbonReport buildReport(BigDecimal totalEmission) {
        CarbonReport report = new CarbonReport();
        report.setTotalEmission(totalEmission);
        report.setDeleted(false);
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }
}
