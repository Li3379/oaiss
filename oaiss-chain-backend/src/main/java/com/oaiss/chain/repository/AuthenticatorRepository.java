package com.oaiss.chain.repository;

import com.oaiss.chain.entity.Authenticator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 认证机构数据访问层
 * 
 * @author OAISS Team
 */
@Repository
public interface AuthenticatorRepository extends JpaRepository<Authenticator, Long> {

    /**
     * 根据用户ID查找
     */
    Optional<Authenticator> findByUserIdAndDeletedFalse(Long userId);

    /**
     * 根据机构编码查找
     */
    Optional<Authenticator> findByOrgCode(String orgCode);
}
