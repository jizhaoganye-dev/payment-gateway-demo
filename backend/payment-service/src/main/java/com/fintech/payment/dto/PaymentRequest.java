package com.fintech.payment.dto;

import com.fintech.payment.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 決済リクエスト DTO
 * 
 * 【バリデーション設計】
 * - jakarta.validation による厳格な入力検証
 * - 金融システムに必要な制約を全て適用
 * - エラーメッセージは日本語で分かりやすく
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "決済リクエスト")
public class PaymentRequest {

    /**
     * 決済金額
     * 
     * 制約:
     * - 必須
     * - 1以上（マイナス・ゼロ禁止）
     * - 最大10億未満
     * - 小数点以下4桁まで
     */
    @NotNull(message = "金額は必須です")
    @DecimalMin(value = "1", message = "金額は1以上である必要があります")
    @DecimalMax(value = "999999999.9999", message = "金額は10億未満である必要があります")
    @Digits(integer = 9, fraction = 4, message = "金額は整数部9桁、小数部4桁以内である必要があります")
    @Schema(description = "決済金額", example = "10000.00", required = true)
    private BigDecimal amount;

    /**
     * 通貨コード (ISO 4217)
     * 
     * 制約:
     * - 必須
     * - 3文字の英大文字
     */
    @NotBlank(message = "通貨コードは必須です")
    @Size(min = 3, max = 3, message = "通貨コードは3文字である必要があります")
    @Pattern(regexp = "^[A-Z]{3}$", message = "通貨コードは3文字の英大文字である必要があります（例: JPY, USD）")
    @Schema(description = "通貨コード (ISO 4217)", example = "JPY", required = true)
    private String currency;

    /**
     * 決済方法
     */
    @NotNull(message = "決済方法は必須です")
    @Schema(description = "決済方法", example = "CREDIT_CARD", required = true)
    private PaymentMethod paymentMethod;

    /**
     * 加盟店ID
     * 
     * 制約:
     * - 必須
     * - 1〜50文字
     * - 英数字とアンダースコア、ハイフンのみ
     */
    @NotBlank(message = "加盟店IDは必須です")
    @Size(min = 1, max = 50, message = "加盟店IDは1〜50文字である必要があります")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "加盟店IDは英数字、アンダースコア、ハイフンのみ使用可能です")
    @Schema(description = "加盟店ID", example = "MERCHANT_001", required = true)
    private String merchantId;

    /**
     * 顧客ID
     * 
     * 制約:
     * - 必須
     * - 1〜50文字
     */
    @NotBlank(message = "顧客IDは必須です")
    @Size(min = 1, max = 50, message = "顧客IDは1〜50文字である必要があります")
    @Schema(description = "顧客ID", example = "CUSTOMER_001", required = true)
    private String customerId;

    /**
     * 決済説明（オプション）
     * 
     * 制約:
     * - 最大500文字
     */
    @Size(max = 500, message = "説明は500文字以内である必要があります")
    @Schema(description = "決済説明", example = "商品購入")
    private String description;

    /**
     * メタデータ（オプション）
     * 加盟店が自由に使用可能なJSON形式のデータ
     */
    @Size(max = 2000, message = "メタデータは2000文字以内である必要があります")
    @Schema(description = "メタデータ（JSON形式）", example = "{\"orderId\": \"ORD-12345\"}")
    private String metadata;
}
