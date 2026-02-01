package com.fintech.payment.dto;

import com.fintech.payment.entity.PaymentMethod;
import com.fintech.payment.entity.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 決済レスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "決済レスポンス")
public class PaymentResponse {

    @Schema(description = "トランザクションID", example = "txn_abc123def456")
    private String transactionId;

    @Schema(description = "決済金額", example = "10000.00")
    private BigDecimal amount;

    @Schema(description = "通貨コード", example = "JPY")
    private String currency;

    @Schema(description = "決済ステータス", example = "COMPLETED")
    private TransactionStatus status;

    @Schema(description = "決済方法", example = "CREDIT_CARD")
    private PaymentMethod paymentMethod;

    @Schema(description = "加盟店ID", example = "MERCHANT_001")
    private String merchantId;

    @Schema(description = "顧客ID", example = "CUSTOMER_001")
    private String customerId;

    @Schema(description = "決済説明", example = "商品購入")
    private String description;

    @Schema(description = "エラーコード（失敗時）", example = "INSUFFICIENT_FUNDS")
    private String errorCode;

    @Schema(description = "エラーメッセージ（失敗時）", example = "残高不足です")
    private String errorMessage;

    @Schema(description = "作成日時", example = "2026-02-01T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "処理完了日時", example = "2026-02-01T10:30:01")
    private LocalDateTime processedAt;
}
