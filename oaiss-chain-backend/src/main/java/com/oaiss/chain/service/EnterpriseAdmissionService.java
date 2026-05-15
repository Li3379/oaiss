package com.oaiss.chain.service;

import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.entity.EnterpriseAdmission;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.EnterpriseAdmissionRepository;
import com.oaiss.chain.repository.EnterpriseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 企业准入证书 Service
 *
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseAdmissionService {

    private final EnterpriseAdmissionRepository enterpriseAdmissionRepository;
    private final EnterpriseRepository enterpriseRepository;

    private static final int MAX_CERT_NO_RETRIES = 3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();

    /**
     * 签发准入证书
     *
     * @param enterpriseId 企业ID
     * @return 签发的准入证书
     */
    @Transactional
    public EnterpriseAdmission issueCertificate(Long enterpriseId) {
        // 1. Verify enterprise exists
        enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> BusinessException.notFound("error.enterprise.notFound"));

        // 2. Check duplicate ACTIVE (D-07)
        enterpriseAdmissionRepository.findFirstByEnterpriseIdAndStatusAndDeletedFalse(enterpriseId, 1)
                .ifPresent(existing -> {
                    throw BusinessException.of(ErrorCode.PARAM_ERROR, "error.admission.alreadyActive");
                });

        // 3. Generate certificateNo with collision retry (D-04)
        String certNo = generateUniqueCertificateNo();

        // 4. Build and save
        EnterpriseAdmission admission = EnterpriseAdmission.builder()
                .enterpriseId(enterpriseId)
                .certificateNo(certNo)
                .issuedDate(LocalDate.now())
                .status(1)
                .build();
        admission = enterpriseAdmissionRepository.save(admission);
        log.info("Enterprise admission issued: {} for enterprise {}", certNo, enterpriseId);
        return admission;
    }

    /**
     * 吊销准入证书
     *
     * @param enterpriseId 企业ID
     */
    @Transactional
    public void revokeCertificate(Long enterpriseId) {
        // Find ACTIVE certificate (D-08)
        EnterpriseAdmission admission = enterpriseAdmissionRepository
                .findFirstByEnterpriseIdAndStatusAndDeletedFalse(enterpriseId, 1)
                .orElseThrow(() -> BusinessException.notFound("error.admission.notFound"));

        admission.setStatus(2); // REVOKED
        enterpriseAdmissionRepository.save(admission);
        log.info("Enterprise admission revoked for enterprise {}", enterpriseId);
    }

    /**
     * 查询准入证书列表（分页，可选状态筛选）
     *
     * @param status 状态筛选（可选）
     * @param page   页码（从1开始）
     * @param size   每页数量
     * @return 分页结果
     */
    public Page<EnterpriseAdmission> listCertificates(Integer status, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return enterpriseAdmissionRepository.findByStatusAndDeletedFalse(status, pageable);
        }
        return enterpriseAdmissionRepository.findByDeletedFalse(pageable);
    }

    /**
     * 查询企业自身的准入证书
     *
     * @param enterpriseId 企业ID
     * @return 该企业的所有准入证书
     */
    public List<EnterpriseAdmission> getMyCertificate(Long enterpriseId) {
        return enterpriseAdmissionRepository.findByEnterpriseIdAndDeletedFalse(enterpriseId);
    }

    /**
     * 生成唯一证书编号: EA-{yyyyMMdd}-{6位随机数字}
     */
    private String generateUniqueCertificateNo() {
        for (int i = 0; i < MAX_CERT_NO_RETRIES; i++) {
            String certNo = String.format("EA-%s-%06d",
                    DATE_FORMAT.format(LocalDate.now()),
                    RANDOM.nextInt(1000000));
            if (!enterpriseAdmissionRepository.existsByCertificateNoAndDeletedFalse(certNo)) {
                return certNo;
            }
        }
        throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "error.admission.certNoGenerationFailed");
    }
}
