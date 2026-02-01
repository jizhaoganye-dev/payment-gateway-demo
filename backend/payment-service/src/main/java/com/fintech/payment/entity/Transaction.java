package com.fintech.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 決済トランザクションエンティティ
 * 
 * 【設計思想】
 * - 金融グレードの精度: BigDecimalによる金額計算
 * - 監査証跡: 作成日時・更新日時の自動記録
 * - 冪等性: トランザクションIDによる重複処理防止
 * 
 * 【リファクタリング履歴】
 * BEFORE (モノリス時代):
 *   - トランザクションと顧客情報が同一テーブル
 *   - ステータス管理がString型で型安全性なし
 *   - 監査ログが手動実装
 * 
 * AFTER (マイクロサービス化):
 *   - 責務分離: トランザクションデータのみに集中
 *   - Enum型によるステータス管理
 *   - JPA Auditingによる自動監査
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_id", columnList = "transactionId", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 外部公開用トランザクションID
     * クライアント側で冪等性を保証するために使用
     */
    @Column(nullable = false, unique = true, length = 36)
    private String transactionId;

    /**
     * 決済金額
     * 金融計算のため、BigDecimalを使用（浮動小数点の誤差を回避）
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * 通貨コード (ISO 4217)
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * 決済ステータス
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /**
     * 決済方法
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /**
     * 加盟店ID
     */
    @Column(nullable = false, length = 50)
    private String merchantId;

    /**
     * 顧客ID（外部参照）
     */
    @Column(nullable = false, length = 50)
    private String customerId;

    /**
     * 決済説明
     */
    @Column(length = 500)
    private String description;

    /**
     * メタデータ (JSON形式)
     * 拡張性のため、追加情報を柔軟に格納
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * エラーコード（失敗時）
     */
    @Column(length = 20)
    private String errorCode;

    /**
     * エラーメッセージ（失敗時）
     */
    @Column(length = 500)
    private String errorMessage;

    /**
     * 処理完了日時
     */
    private LocalDateTime processedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * トランザクション作成前の初期化処理
     */
    @PrePersist
    public void prePersist() {
        if (this.transactionId == null) {
            this.transactionId = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = TransactionStatus.PENDING;
        }
    }
}
