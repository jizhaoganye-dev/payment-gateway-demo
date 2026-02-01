package com.fintech.payment.exception;

/**
 * 無効なカード情報例外
 */
public class InvalidCardException extends PaymentException {

    public InvalidCardException(String message) {
        super("INVALID_CARD", message);
    }
}
