package com.fintech.payment.exception;

import lombok.Getter;

/**
 * 決済関連例外の基底クラス
 * 
 * 【設計思想】
 * - 全ての決済例外にエラーコードを付与
 * - 機械可読なコード + 人間可読なメッセージ
 * - エラー追跡とデバッグを容易に
 */
@Getter
public class PaymentException extends RuntimeException {

    private final String errorCode;

    public PaymentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PaymentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
