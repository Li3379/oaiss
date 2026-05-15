package com.oaiss.chain.service;

import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.dto.PowerGridCalculationRequest;
import com.oaiss.chain.dto.PowerGridCalculationResponse;
import com.oaiss.chain.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 电网碳排放公式计算服务测试
 */
@ExtendWith(MockitoExtension.class)
class PowerGridFormulaServiceTest {

    @InjectMocks
    private PowerGridFormulaService powerGridFormulaService;

    private PowerGridCalculationRequest buildRequest(
            BigDecimal transmissionVolume,
            BigDecimal lineLossRate,
            BigDecimal gridEmissionFactor,
            BigDecimal importedElectricity,
            BigDecimal importEmissionFactor) {
        PowerGridCalculationRequest request = new PowerGridCalculationRequest();
        request.setTransmissionVolume(transmissionVolume);
        request.setLineLossRate(lineLossRate);
        request.setGridEmissionFactor(gridEmissionFactor);
        request.setImportedElectricity(importedElectricity);
        request.setImportEmissionFactor(importEmissionFactor);
        request.setReportingYear(2024);
        request.setEnterpriseName("测试企业");
        return request;
    }

    @Test
    @DisplayName("全零输入 → 总排放为0")
    void calculate_allZeros_returnsZeroEmission() {
        PowerGridCalculationRequest request = buildRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);

        PowerGridCalculationResponse response = powerGridFormulaService.calculate(request);

        assertEquals(0, response.getTotalEmission().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.getTransmissionLossEmission().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.getImportedEmission().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.getTransmissionLoss().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("基本输配损耗计算: V=10000, L_rate=0.05, EF=0.6 → T_loss=500, E_loss=300")
    void calculate_basicTransmissionLoss_returnsCorrectEmission() {
        PowerGridCalculationRequest request = buildRequest(
                new BigDecimal("10000"), new BigDecimal("0.05"),
                new BigDecimal("0.6"), null, null);

        PowerGridCalculationResponse response = powerGridFormulaService.calculate(request);

        assertEquals(0, new BigDecimal("500.0000").compareTo(response.getTransmissionLoss()));
        assertEquals(0, new BigDecimal("300.0000").compareTo(response.getTransmissionLossEmission()));
        assertEquals(0, response.getImportedEmission().compareTo(BigDecimal.ZERO));
        assertEquals(0, new BigDecimal("300.0000").compareTo(response.getTotalEmission()));
    }

    @Test
    @DisplayName("含外购电力: imported=2000, importEF=0.8 → importedEmission=1600, total=1900")
    void calculate_withImportedEmission_returnsCorrectTotal() {
        PowerGridCalculationRequest request = buildRequest(
                new BigDecimal("10000"), new BigDecimal("0.05"),
                new BigDecimal("0.6"), new BigDecimal("2000"), new BigDecimal("0.8"));

        PowerGridCalculationResponse response = powerGridFormulaService.calculate(request);

        assertEquals(0, new BigDecimal("300.0000").compareTo(response.getTransmissionLossEmission()));
        assertEquals(0, new BigDecimal("1600.0000").compareTo(response.getImportedEmission()));
        assertEquals(0, new BigDecimal("1900.0000").compareTo(response.getTotalEmission()));
    }

    @Test
    @DisplayName("线损率超过1 → 抛出BusinessException")
    void calculate_lineLossRateExceedsOne_throwsBusinessException() {
        PowerGridCalculationRequest request = buildRequest(
                new BigDecimal("10000"), new BigDecimal("1.5"),
                new BigDecimal("0.6"), null, null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> powerGridFormulaService.calculate(request));
        assertEquals(ErrorCode.DATA_OUT_OF_RANGE, exception.getCode());
    }

    @Test
    @DisplayName("负输配电量 → 抛出BusinessException")
    void calculate_negativeTransmissionVolume_throwsBusinessException() {
        // Bean Validation would normally reject this, but test service-layer defense
        PowerGridCalculationRequest request = new PowerGridCalculationRequest();
        request.setTransmissionVolume(new BigDecimal("-100"));
        request.setLineLossRate(new BigDecimal("0.05"));
        request.setGridEmissionFactor(new BigDecimal("0.6"));
        request.setReportingYear(2024);
        request.setEnterpriseName("测试企业");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> powerGridFormulaService.calculate(request));
        assertEquals(ErrorCode.DATA_OUT_OF_RANGE, exception.getCode());
    }

    @Test
    @DisplayName("可选参数为null → 视为零，不报错")
    void calculate_nullOptionalParams_treatedAsZero() {
        PowerGridCalculationRequest request = new PowerGridCalculationRequest();
        request.setTransmissionVolume(new BigDecimal("5000"));
        request.setLineLossRate(new BigDecimal("0.04"));
        request.setGridEmissionFactor(new BigDecimal("0.5"));
        request.setReportingYear(2024);
        request.setEnterpriseName("测试企业");
        // importedElectricity, importEmissionFactor, generationVolume, exportedElectricity all null

        PowerGridCalculationResponse response = powerGridFormulaService.calculate(request);

        assertEquals(0, response.getImportedEmission().compareTo(BigDecimal.ZERO));
        assertEquals(0, new BigDecimal("200.0000").compareTo(response.getTransmissionLoss()));
        assertEquals(0, new BigDecimal("100.0000").compareTo(response.getTransmissionLossEmission()));
        assertEquals(0, new BigDecimal("100.0000").compareTo(response.getTotalEmission()));
        assertEquals("GB/T 32150-2015", response.getFormulaReference());
        assertEquals("2024", response.getReportingYear());
        assertEquals("测试企业", response.getEnterpriseName());
        assertNotNull(response.getCalculatedAt());
    }
}
