package com.fintech.payment.exception;

/**
 * トランザクション未検出例外
 */
public class TransactionNotFoundException extends RuntimeException {

    private final String transactionId;

    public TransactionNotFoundException(String transactionId) {
        super("トランザクションが見つかりません: " + transactionId);
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
