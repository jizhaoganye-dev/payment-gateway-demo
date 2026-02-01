package com.fintech.payment.exception;

import com.fintech.payment.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * グローバル例外ハンドラー
 * 
 * 【設計思想】
 * - 統一されたエラーレスポンス形式
 * - 適切なHTTPステータスコード
 * - セキュアなエラーメッセージ（内部情報の漏洩防止）
 * 
 * 【リファクタリング履歴】
 * BEFORE: 各Controllerで個別に例外処理 → 重複コード、一貫性なし
 * AFTER: 集約された例外ハンドリング → DRY原則、統一レスポンス
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 決済例外ハンドリング
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentException(PaymentException ex) {
        log.error("決済エラー: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * トランザクション未検出例外ハンドリング
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionNotFoundException(TransactionNotFoundException ex) {
        log.warn("トランザクション未検出: {}", ex.getTransactionId());
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("TRANSACTION_NOT_FOUND", ex.getMessage()));
    }

    /**
     * バリデーションエラーハンドリング
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("バリデーションエラー: {}", errors);
        
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .errorCode("VALIDATION_ERROR")
                .message("入力値が不正です")
                .data(errors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 予期せぬ例外ハンドリング
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("予期せぬエラー: ", ex);
        
        // 本番環境では詳細なエラーメッセージを隠す
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "システムエラーが発生しました。しばらく経ってから再度お試しください。"));
    }
}
