package com.fintech.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fintech.payment.entity.PaymentMethod;
import com.fintech.payment.entity.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 決済レスポンス DTO
 * 
 * 【設計思想】
 * - Entity の内部構造を外部に露出しない
 * - クライアントに必要な情報のみを返却
 * - null フィールドはJSON出力から除外
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "決済レスポンス")
public class PaymentResponse {

    @Schema(description = "トランザクションID", example = "txn_a1b2c3d4")
    private String transactionId;

    @Schema(description = "決済金額", example = "10000.00")
    private BigDecimal amount;

    @Schema(description = "通貨コード", example = "JPY")
    private String currency;

    @Schema(description = "トランザクションステータス", example = "COMPLETED")
    private TransactionStatus status;

    @Schema(description = "決済方法", example = "CREDIT_CARD")
    private PaymentMethod paymentMethod;

    @Schema(description = "加盟店ID", example = "MERCHANT_001")
    private String merchantId;

    @Schema(description = "顧客ID", example = "CUSTOMER_001")
    private String customerId;

    @Schema(description = "決済説明", example = "商品購入")
    private String description;

    @Schema(description = "エラーコード（失敗時のみ）", example = "INSUFFICIENT_FUNDS")
    private String errorCode;

    @Schema(description = "エラーメッセージ（失敗時のみ）", example = "残高が不足しています")
    private String errorMessage;

    @Schema(description = "作成日時", example = "2026-02-01T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "処理完了日時", example = "2026-02-01T10:30:01")
    private LocalDateTime processedAt;

    @Schema(description = "更新日時", example = "2026-02-01T10:30:01")
    private LocalDateTime updatedAt;
}
