package com.fintech.payment.exception;

import lombok.Getter;

/**
 * トランザクション未検出例外
 */
@Getter
public class TransactionNotFoundException extends RuntimeException {

    private final String transactionId;

    public TransactionNotFoundException(String transactionId) {
        super("トランザクションが見つかりません: " + transactionId);
        this.transactionId = transactionId;
    }
}
