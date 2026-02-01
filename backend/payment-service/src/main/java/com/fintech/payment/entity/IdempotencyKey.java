package com.fintech.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 冪等性キー エンティティ
 * 
 * 【設計思想】
 * 金融システムにおいて、ネットワーク障害やタイムアウト時に
 * クライアントがリトライした場合でも、二重決済を防止する仕組み
 * 
 * 【動作原理】
 * 1. クライアントはリクエストヘッダーに一意の Idempotency-Key を付与
 * 2. サーバーは同一キーでの処理結果をDBに保存
 * 3. 同一キーで再リクエストがあった場合、保存済みのレスポンスを返却
 * 
 * 【金融業界標準】
 * - Stripe, PayPal等の決済APIで広く採用されている設計パターン
 * - RFC 7231 に基づく冪等性の実装
 */
@Entity
@Table(name = "idempotency_keys", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 冪等性キー（クライアント指定）
     * 通常はUUID形式
     */
    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    /**
     * 関連するトランザクションID
     */
    @Column(length = 36)
    private String transactionId;

    /**
     * 処理ステータス
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    /**
     * キャッシュされたレスポンス（JSON形式）
     * 同一キーで再リクエストがあった場合に返却
     */
    @Column(columnDefinition = "TEXT")
    private String cachedResponse;

    /**
     * リクエストハッシュ（整合性検証用）
     * 同一キーで異なるリクエスト内容の場合はエラー
     */
    @Column(length = 64)
    private String requestHash;

    /**
     * 有効期限（通常24時間）
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 冪等性キーステータス
     */
    public enum IdempotencyStatus {
        /**
         * 処理中
         */
        PROCESSING,
        
        /**
         * 完了（レスポンスキャッシュ済み）
         */
        COMPLETED,
        
        /**
         * 失敗
         */
        FAILED
    }

    /**
     * 有効期限切れかどうかを判定
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 処理完了かどうかを判定
     */
    public boolean isCompleted() {
        return this.status == IdempotencyStatus.COMPLETED;
    }
}
