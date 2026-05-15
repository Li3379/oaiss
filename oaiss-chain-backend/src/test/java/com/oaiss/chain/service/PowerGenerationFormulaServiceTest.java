package com.oaiss.chain.service;

import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.dto.PowerGenerationCalculationRequest;
import com.oaiss.chain.dto.PowerGenerationCalculationResponse;
import com.oaiss.chain.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 发电企业碳排放公式计算服务测试（GB/T 32150-2015）
 *
 * @author OAISS Team
 */
@ExtendWith(MockitoExtension.class)
class PowerGenerationFormulaServiceTest {

    @InjectMocks
    private PowerGenerationFormulaService service;

    private PowerGenerationCalculationRequest baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = new PowerGenerationCalculationRequest();
        baseRequest.setReportingYear(2024);
        baseRequest.setEnterpriseName("测试发电企业");
        // All fuel params left null (treated as zero)
    }

    @Test
    @DisplayName("所有燃料参数为零时，总排放量应为0")
    void calculate_allZeros_returnsZeroEmission() {
        // All fuel params are null → emission = 0

        PowerGenerationCalculationResponse response = service.calculate(baseRequest);

        assertEquals(0, response.getTotalEmission().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.getCombustionEmission().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.getDesulfurizationEmission().compareTo(BigDecimal.ZERO));
        assertTrue(response.getFuelDetails().isEmpty());
        assertEquals("2024", response.getReportingYear());
        assertEquals("测试发电企业", response.getEnterpriseName());
        assertEquals("GB/T 32150-2015", response.getFormulaReference());
    }

    @Test
    @DisplayName("已知值计算: 原煤 FC=100, NCV=20, CC=0.025, OF=0.98 → 燃烧排放≈179.6667")
    void calculate_knownValues_correctCombustionEmission() {
        baseRequest.setRawCoalFc(new BigDecimal("100"));
        baseRequest.setRawCoalNcv(new BigDecimal("20"));
        baseRequest.setRawCoalCc(new BigDecimal("0.025"));
        baseRequest.setRawCoalOf(new BigDecimal("0.98"));

        // Expected: 100 × 20 × 0.025 × 0.98 × 44/12
        BigDecimal expected = new BigDecimal("100")
                .multiply(new BigDecimal("20"))
                .multiply(new BigDecimal("0.025"))
                .multiply(new BigDecimal("0.98"))
                .multiply(new BigDecimal("44").divide(new BigDecimal("12"), 4, RoundingMode.HALF_UP))
                .setScale(4, RoundingMode.HALF_UP);

        PowerGenerationCalculationResponse response = service.calculate(baseRequest);

        assertEquals(0, response.getCombustionEmission().compareTo(expected));
        assertEquals(0, response.getTotalEmission().compareTo(expected));
        assertEquals(1, response.getFuelDetails().size());
        assertEquals("原煤", response.getFuelDetails().get(0).getFuelType());
    }

    @Test
    @DisplayName("含脱硫参数时，总排放 = 燃烧排放 + 脱硫排放")
    void calculate_withDesulfurization_totalIsSumOfBoth() {
        baseRequest.setRawCoalFc(new BigDecimal("100"));
        baseRequest.setRawCoalNcv(new BigDecimal("20"));
        baseRequest.setRawCoalCc(new BigDecimal("0.025"));
        baseRequest.setRawCoalOf(new BigDecimal("0.98"));

        baseRequest.setCarbonateConsumed(new BigDecimal("10"));
        baseRequest.setDesulfEmissionFactor(new BigDecimal("0.44"));
        baseRequest.setDesulfConversionRate(new BigDecimal("1.0"));

        PowerGenerationCalculationResponse response = service.calculate(baseRequest);

        // Desulfurization: 10 × 0.44 × 1.0 = 4.4000
        BigDecimal expectedDesulf = new BigDecimal("10")
                .multiply(new BigDecimal("0.44"))
                .multiply(new BigDecimal("1.0"))
                .setScale(4, RoundingMode.HALF_UP);
        assertEquals(0, response.getDesulfurizationEmission().compareTo(expectedDesulf));
        assertEquals(0, response.getTotalEmission().compareTo(
                response.getCombustionEmission().add(response.getDesulfurizationEmission())));
    }

    @Test
    @DisplayName("负燃料消耗量应抛出BusinessException")
    void calculate_negativeFc_throwsBusinessException() {
        baseRequest.setRawCoalFc(new BigDecimal("-5"));
        baseRequest.setRawCoalNcv(new BigDecimal("20"));
        baseRequest.setRawCoalCc(new BigDecimal("0.025"));
        baseRequest.setRawCoalOf(new BigDecimal("0.98"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.calculate(baseRequest));
        assertEquals(ErrorCode.DATA_OUT_OF_RANGE, exception.getCode());
    }

    @Test
    @DisplayName("碳氧化率大于1应抛出BusinessException")
    void calculate_ofGreaterThanOne_throwsBusinessException() {
        baseRequest.setRawCoalFc(new BigDecimal("100"));
        baseRequest.setRawCoalNcv(new BigDecimal("20"));
        baseRequest.setRawCoalCc(new BigDecimal("0.025"));
        baseRequest.setRawCoalOf(new BigDecimal("1.5"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.calculate(baseRequest));
        assertEquals(ErrorCode.DATA_OUT_OF_RANGE, exception.getCode());
    }

    @Test
    @DisplayName("null燃料参数视为零，不报错")
    void calculate_nullFuelParams_treatedAsZero() {
        // rawCoalFc is null, but NCV/CC/OF are set — should treat as zero emission
        baseRequest.setRawCoalNcv(new BigDecimal("20"));
        baseRequest.setRawCoalCc(new BigDecimal("0.025"));
        baseRequest.setRawCoalOf(new BigDecimal("0.98"));

        PowerGenerationCalculationResponse response = service.calculate(baseRequest);

        assertEquals(0, response.getCombustionEmission().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.getTotalEmission().compareTo(BigDecimal.ZERO));
        assertTrue(response.getFuelDetails().isEmpty());
    }
}
