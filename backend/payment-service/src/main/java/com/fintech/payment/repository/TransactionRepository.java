package com.fintech.payment.repository;

import com.fintech.payment.entity.Transaction;
import com.fintech.payment.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * トランザクションリポジトリ
 * 
 * 【設計思想】
 * - Spring Data JPAによる効率的なクエリ生成
 * - カスタムクエリによる複雑な検索対応
 * - ページネーション対応
 * 
 * 【リファクタリング履歴】
 * BEFORE: 手書きJDBCクエリ → SQLインジェクションリスク、ボイラープレート多い
 * AFTER: Spring Data JPA → 型安全、自動クエリ生成、保守性向上
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * トランザクションIDで検索
     */
    Optional<Transaction> findByTransactionId(String transactionId);

    /**
     * 加盟店IDでトランザクション一覧取得（ページング）
     */
    Page<Transaction> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    /**
     * 顧客IDでトランザクション一覧取得（ページング）
     */
    Page<Transaction> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);

    /**
     * ステータスでトランザクション一覧取得
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * 期間内のトランザクション取得
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 加盟店の売上合計取得
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.merchantId = :merchantId AND t.status = 'COMPLETED'")
    BigDecimal getTotalSalesByMerchant(@Param("merchantId") String merchantId);

    /**
     * 期間内の売上合計取得
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'COMPLETED' AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSalesByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * ステータス別トランザクション数取得
     */
    @Query("SELECT t.status, COUNT(t) FROM Transaction t GROUP BY t.status")
    List<Object[]> countByStatus();

    /**
     * 処理中のトランザクション取得（タイムアウト監視用）
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PROCESSING' AND t.createdAt < :timeout")
    List<Transaction> findStaleProcessingTransactions(@Param("timeout") LocalDateTime timeout);
}
