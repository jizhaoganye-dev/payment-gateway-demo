package com.fintech.payment.exception;

import com.fintech.payment.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * グローバル例外ハンドラー
 * 
 * 【設計思想】
 * - 全てのエラーを統一されたJSON形式で返却
 * - 機械可読なエラーコード（APIクライアント向け）
 * - 人間可読なメッセージ（開発者向け）
 * - 本番環境ではスタックトレースを隠蔽
 * 
 * 【対応HTTPステータス】
 * - 400 Bad Request: バリデーションエラー、不正なリクエスト
 * - 404 Not Found: リソース未検出
 * - 405 Method Not Allowed: 許可されていないHTTPメソッド
 * - 409 Conflict: 冪等性キー衝突
 * - 415 Unsupported Media Type: サポートされていないContent-Type
 * - 500 Internal Server Error: 予期せぬエラー
 * - 503 Service Unavailable: タイムアウト
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${app.debug:false}")
    private boolean debugMode;

    // =========================================================================
    // 業務例外（400系）
    // =========================================================================

    /**
     * 決済例外ハンドリング
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.error("[{}] 決済エラー: code={}, message={}", 
                requestId, ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 残高不足例外ハンドリング
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(
            InsufficientFundsException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] 残高不足: {}", requestId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("INSUFFICIENT_FUNDS")
                .message(ex.getMessage())
                .status(HttpStatus.PAYMENT_REQUIRED.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }

    /**
     * 無効なカード情報例外ハンドリング
     */
    @ExceptionHandler(InvalidCardException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCardException(
            InvalidCardException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] 無効なカード: {}", requestId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("INVALID_CARD")
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * タイムアウト例外ハンドリング
     */
    @ExceptionHandler(PaymentTimeoutException.class)
    public ResponseEntity<ErrorResponse> handlePaymentTimeoutException(
            PaymentTimeoutException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.error("[{}] タイムアウト: {}", requestId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("PAYMENT_TIMEOUT")
                .message(ex.getMessage())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * トランザクション未検出例外ハンドリング
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFoundException(
            TransactionNotFoundException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] トランザクション未検出: {}", requestId, ex.getTransactionId());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("TRANSACTION_NOT_FOUND")
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 冪等性キー衝突例外ハンドリング
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflictException(
            IdempotencyConflictException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] 冪等性キー衝突: {}", requestId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("IDEMPOTENCY_CONFLICT")
                .message(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // =========================================================================
    // バリデーションエラー（400）
    // =========================================================================

    /**
     * Bean Validationエラーハンドリング
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("[{}] バリデーションエラー: {}", requestId, errors);
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("入力値が不正です")
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .fieldErrors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * リクエストパラメータ欠落エラー
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] パラメータ欠落: {}", requestId, ex.getParameterName());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("MISSING_PARAMETER")
                .message("必須パラメータが不足しています: " + ex.getParameterName())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * リクエストヘッダー欠落エラー
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeaderException(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] ヘッダー欠落: {}", requestId, ex.getHeaderName());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("MISSING_HEADER")
                .message("必須ヘッダーが不足しています: " + ex.getHeaderName())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 型変換エラー
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] 型変換エラー: {} (expected: {})", 
                requestId, ex.getName(), ex.getRequiredType());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("TYPE_MISMATCH")
                .message("パラメータの型が不正です: " + ex.getName())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * JSONパースエラー
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] JSONパースエラー: {}", requestId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("INVALID_JSON")
                .message("リクエストボディのJSON形式が不正です")
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // =========================================================================
    // HTTPエラー（4xx）
    // =========================================================================

    /**
     * 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] エンドポイント未検出: {} {}", 
                requestId, ex.getHttpMethod(), ex.getRequestURL());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("NOT_FOUND")
                .message("リクエストされたエンドポイントが見つかりません")
                .status(HttpStatus.NOT_FOUND.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 405 Method Not Allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] HTTPメソッド不許可: {}", requestId, ex.getMethod());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("METHOD_NOT_ALLOWED")
                .message("このエンドポイントでは " + ex.getMethod() + " メソッドはサポートされていません")
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /**
     * 415 Unsupported Media Type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.warn("[{}] サポートされていないMediaType: {}", requestId, ex.getContentType());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("UNSUPPORTED_MEDIA_TYPE")
                .message("サポートされていないContent-Typeです。application/jsonを使用してください")
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    // =========================================================================
    // 予期せぬエラー（500）
    // =========================================================================

    /**
     * 予期せぬ例外ハンドリング
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        log.error("[{}] 予期せぬエラー: ", requestId, ex);
        
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .errorCode("INTERNAL_ERROR")
                .message("システムエラーが発生しました。しばらく経ってから再度お試しください")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI())
                .requestId(requestId);
        
        // デバッグモードの場合のみ詳細を含める
        if (debugMode) {
            builder.debugInfo(ex.getClass().getName() + ": " + ex.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(builder.build());
    }

    // =========================================================================
    // ユーティリティ
    // =========================================================================

    /**
     * リクエストID生成
     */
    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
