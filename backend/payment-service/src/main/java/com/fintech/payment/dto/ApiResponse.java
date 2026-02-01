package com.fintech.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 統一APIレスポンスラッパー
 * 
 * 【設計思想】
 * - 一貫したレスポンス形式
 * - エラー情報の統一的な表現
 * - フロントエンドでの処理を簡素化
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API統一レスポンス")
public class ApiResponse<T> {

    @Schema(description = "処理成功フラグ", example = "true")
    private boolean success;

    @Schema(description = "レスポンスデータ")
    private T data;

    @Schema(description = "エラーコード", example = "PAYMENT_FAILED")
    private String errorCode;

    @Schema(description = "エラーメッセージ", example = "決済処理に失敗しました")
    private String message;

    @Schema(description = "リクエストID（トレーシング用）", example = "req_abc123")
    private String requestId;

    @Schema(description = "レスポンス日時", example = "2026-02-01T10:30:00")
    private LocalDateTime timestamp;

    /**
     * 成功レスポンス生成
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 成功レスポンス生成（メッセージ付き）
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * エラーレスポンス生成
     */
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * エラーレスポンス生成（リクエストID付き）
     */
    public static <T> ApiResponse<T> error(String errorCode, String message, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
