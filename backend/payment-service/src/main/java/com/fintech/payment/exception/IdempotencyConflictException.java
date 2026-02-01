package com.fintech.payment.exception;

/**
 * 冪等性キー衝突例外
 * 
 * 【発生条件】
 * - 同一キーで処理が実行中の場合
 * - 同一キーで異なるリクエスト内容が送信された場合
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
