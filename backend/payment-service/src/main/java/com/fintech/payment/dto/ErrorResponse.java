package com.fintech.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 統一エラーレスポンス
 * 
 * 【設計思想】
 * - RFC 7807 (Problem Details for HTTP APIs) に準拠
 * - 機械可読なエラーコード + 人間可読なメッセージ
 * - デバッグ情報は本番では非表示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "エラーレスポンス")
public class ErrorResponse {

    @Schema(description = "エラーコード", example = "VALIDATION_ERROR")
    private String errorCode;

    @Schema(description = "エラーメッセージ", example = "入力値が不正です")
    private String message;

    @Schema(description = "HTTPステータスコード", example = "400")
    private int status;

    @Schema(description = "リクエストパス", example = "/api/v1/payments")
    private String path;

    @Schema(description = "リクエストID（トレーシング用）", example = "req_abc123")
    private String requestId;

    @Schema(description = "エラー発生日時", example = "2026-02-01T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "フィールドエラー詳細（バリデーションエラー時）")
    private Map<String, String> fieldErrors;

    @Schema(description = "デバッグ情報（開発環境のみ）")
    private String debugInfo;

    /**
     * 基本エラーレスポンス生成
     */
    public static ErrorResponse of(String errorCode, String message, int status, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .status(status)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * バリデーションエラーレスポンス生成
     */
    public static ErrorResponse validationError(Map<String, String> fieldErrors, String path) {
        return ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("入力値が不正です")
                .status(400)
                .path(path)
                .fieldErrors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
