package com.fintech.payment.repository;

import com.fintech.payment.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 冪等性キー リポジトリ
 */
@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    /**
     * 冪等性キーで検索
     */
    Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);

    /**
     * 冪等性キーの存在確認
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * 期限切れキーの削除（定期クリーンアップ用）
     */
    @Modifying
    @Query("DELETE FROM IdempotencyKey ik WHERE ik.expiresAt < :now")
    int deleteExpiredKeys(@Param("now") LocalDateTime now);
}
