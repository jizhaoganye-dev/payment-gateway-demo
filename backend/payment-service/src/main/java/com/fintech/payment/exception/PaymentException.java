package com.fintech.payment.exception;

import lombok.Getter;

/**
 * 決済例外
 * 
 * 【設計思想】
 * - 金融グレードのエラーハンドリング
 * - エラーコードによる機械可読なエラー識別
 * - 詳細なエラーメッセージによるデバッグ支援
 */
@Getter
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final String details;

    public PaymentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public PaymentException(String errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public PaymentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = cause.getMessage();
    }
}
