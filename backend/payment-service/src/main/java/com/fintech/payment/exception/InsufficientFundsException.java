package com.fintech.payment.exception;

/**
 * 残高不足例外
 */
public class InsufficientFundsException extends PaymentException {

    public InsufficientFundsException(String message) {
        super("INSUFFICIENT_FUNDS", message);
    }
}
