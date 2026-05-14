package com.oaiss.chain.repository;

import com.oaiss.chain.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 交易数据访问层
 * 
 * @author OAISS Team
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * 根据交易编号查找
     */
    Optional<Transaction> findByTradeNo(String tradeNo);

    /**
     * 根据卖方ID分页查询
     */
    Page<Transaction> findBySellerIdAndDeletedFalse(Long sellerId, Pageable pageable);

    /**
     * 根据买方ID分页查询
     */
    Page<Transaction> findByBuyerIdAndDeletedFalse(Long buyerId, Pageable pageable);

    /**
     * 统计与指定用户相关的交易数量（作为买方或卖方）
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.deleted = false AND (t.buyerId = :userId OR t.sellerId = :userId)")
    long countByUserIdRelated(@Param("userId") Long userId);

    /**
     * 根据交易类型分页查询
     */
    Page<Transaction> findByTradeTypeAndDeletedFalse(Integer tradeType, Pageable pageable);

    /**
     * 根据状态分页查询
     */
    Page<Transaction> findByStatusAndDeletedFalse(Integer status, Pageable pageable);

    /**
     * 查询未删除的交易（分页）
     */
    Page<Transaction> findByDeletedFalse(Pageable pageable);

    /**
     * 综合搜索
     */
    @Query("SELECT t FROM Transaction t WHERE t.deleted = false " +
            "AND (:sellerId IS NULL OR t.sellerId = :sellerId) " +
            "AND (:buyerId IS NULL OR t.buyerId = :buyerId) " +
            "AND (:tradeType IS NULL OR t.tradeType = :tradeType) " +
            "AND (:status IS NULL OR t.status = :status)")
    Page<Transaction> search(
            @Param("sellerId") Long sellerId,
            @Param("buyerId") Long buyerId,
            @Param("tradeType") Integer tradeType,
            @Param("status") Integer status,
            Pageable pageable);

    /**
     * 查询与指定用户相关的交易（作为买方或卖方）
     */
    @Query("SELECT t FROM Transaction t WHERE t.deleted = false " +
            "AND (t.sellerId = :userId OR t.buyerId = :userId) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:tradeType IS NULL OR t.tradeType = :tradeType)")
    Page<Transaction> findByUserIdRelated(
            @Param("userId") Long userId,
            @Param("tradeType") Integer tradeType,
            @Param("status") Integer status,
            Pageable pageable);
}
