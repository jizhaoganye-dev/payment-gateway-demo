package com.fintech.payment.dto;

import com.fintech.payment.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 決済リクエストDTO
 * 
 * 【バリデーション設計】
 * - Bean Validationによる入力検証
 * - 金融グレードの厳格なバリデーション
 * - 明確なエラーメッセージ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "決済リクエスト")
public class PaymentRequest {

    @Schema(description = "決済金額", example = "10000.00", required = true)
    @NotNull(message = "金額は必須です")
    @DecimalMin(value = "1.00", message = "金額は1以上である必要があります")
    @DecimalMax(value = "99999999.99", message = "金額が上限を超えています")
    @Digits(integer = 8, fraction = 2, message = "金額の形式が不正です")
    private BigDecimal amount;

    @Schema(description = "通貨コード (ISO 4217)", example = "JPY", required = true)
    @NotBlank(message = "通貨コードは必須です")
    @Size(min = 3, max = 3, message = "通貨コードは3文字である必要があります")
    @Pattern(regexp = "^[A-Z]{3}$", message = "通貨コードの形式が不正です")
    private String currency;

    @Schema(description = "決済方法", example = "CREDIT_CARD", required = true)
    @NotNull(message = "決済方法は必須です")
    private PaymentMethod paymentMethod;

    @Schema(description = "加盟店ID", example = "MERCHANT_001", required = true)
    @NotBlank(message = "加盟店IDは必須です")
    @Size(max = 50, message = "加盟店IDは50文字以内である必要があります")
    private String merchantId;

    @Schema(description = "顧客ID", example = "CUSTOMER_001", required = true)
    @NotBlank(message = "顧客IDは必須です")
    @Size(max = 50, message = "顧客IDは50文字以内である必要があります")
    private String customerId;

    @Schema(description = "決済説明", example = "商品購入", required = false)
    @Size(max = 500, message = "説明は500文字以内である必要があります")
    private String description;

    @Schema(description = "冪等性キー（重複リクエスト防止）", example = "unique-request-id-123", required = false)
    @Size(max = 64, message = "冪等性キーは64文字以内である必要があります")
    private String idempotencyKey;

    @Schema(description = "メタデータ (JSON形式)", example = "{\"orderId\": \"ORD-001\"}", required = false)
    private String metadata;
}
