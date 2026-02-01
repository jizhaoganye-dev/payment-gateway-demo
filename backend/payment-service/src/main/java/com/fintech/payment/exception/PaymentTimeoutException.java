package com.fintech.payment.exception;

/**
 * 決済タイムアウト例外
 */
public class PaymentTimeoutException extends PaymentException {

    public PaymentTimeoutException(String message) {
        super("PAYMENT_TIMEOUT", message);
    }
}
