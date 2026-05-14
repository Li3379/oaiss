package com.oaiss.chain.repository;

import com.oaiss.chain.entity.CarbonReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 碳报告数据访问层
 * 
 * @author OAISS Team
 */
@Repository
public interface CarbonReportRepository extends JpaRepository<CarbonReport, Long> {

    /**
     * 根据报告编号查找
     */
    Optional<CarbonReport> findByReportNo(String reportNo);

    /**
     * 根据企业ID分页查询
     */
    List<CarbonReport> findByEnterpriseIdAndDeletedFalse(Long enterpriseId);

    Page<CarbonReport> findByEnterpriseIdAndDeletedFalse(Long enterpriseId, Pageable pageable);

    /**
     * 根据状态分页查询
     */
    Page<CarbonReport> findByStatusAndDeletedFalse(Integer status, Pageable pageable);

    /**
     * 根据审核员ID分页查询
     */
    Page<CarbonReport> findByReviewerIdAndDeletedFalse(Long reviewerId, Pageable pageable);

    /**
     * 根据企业ID和状态查询
     */
    Page<CarbonReport> findByEnterpriseIdAndStatusAndDeletedFalse(
            Long enterpriseId, Integer status, Pageable pageable);

    /**
     * 查询所有未删除的报告
     */
    Page<CarbonReport> findByDeletedFalse(Pageable pageable);

    /**
     * 综合搜索
     */
    @Query("SELECT r FROM CarbonReport r WHERE r.deleted = false " +
            "AND (:enterpriseId IS NULL OR r.enterpriseId = :enterpriseId) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:keyword IS NULL OR r.title LIKE %:keyword% OR r.reportNo LIKE %:keyword%)")
    Page<CarbonReport> search(
            @Param("enterpriseId") Long enterpriseId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 统计指定状态的报告数量
     */
    long countByStatusAndDeletedFalse(Integer status);

    /**
     * 根据多个状态分页查询
     */
    Page<CarbonReport> findByStatusInAndDeletedFalse(List<Integer> statuses, Pageable pageable);
}
