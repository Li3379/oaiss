package com.oaiss.chain.repository;

import com.oaiss.chain.entity.CarbonNeutralProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 碳中和项目 Repository
 *
 * @author OAISS Team
 */
@Repository
public interface CarbonNeutralProjectRepository extends JpaRepository<CarbonNeutralProject, Long> {

    Optional<CarbonNeutralProject> findByProjectNo(String projectNo);

    Page<CarbonNeutralProject> findByOwnerIdAndDeletedFalse(Long ownerId, Pageable pageable);

    Page<CarbonNeutralProject> findByProjectTypeAndDeletedFalse(Integer projectType, Pageable pageable);

    @Query("SELECT p FROM CarbonNeutralProject p WHERE p.deleted = false " +
            "AND (:projectType IS NULL OR p.projectType = :projectType) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:keyword IS NULL OR p.projectName LIKE %:keyword% OR p.projectNo LIKE %:keyword%)")
    Page<CarbonNeutralProject> search(
            @Param("projectType") Integer projectType,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            Pageable pageable);

    Page<CarbonNeutralProject> findByOwnerIdAndStatusAndDeletedFalse(Long ownerId, Integer status, Pageable pageable);

    Page<CarbonNeutralProject> findByVerifierIdAndVerificationStatusAndDeletedFalse(Long verifierId, Integer verificationStatus, Pageable pageable);
}
