package com.fintech.payment.entity;

/**
 * トランザクションステータス
 * 
 * 【状態遷移図】
 * PENDING → PROCESSING → COMPLETED
 *                ↓
 *              FAILED
 *                ↓
 *             REFUNDED (部分的または全額)
 *             CANCELLED
 * 
 * 【リファクタリング履歴】
 * BEFORE: String型でステータス管理 → タイポや無効値のリスク
 * AFTER: Enum型で型安全性を確保 → コンパイル時にエラー検出
 */
public enum TransactionStatus {
    
    /**
     * 処理待ち - トランザクション作成直後
     */
    PENDING("処理待ち"),
    
    /**
     * 処理中 - 決済プロセッサーへ送信中
     */
    PROCESSING("処理中"),
    
    /**
     * 完了 - 決済成功
     */
    COMPLETED("完了"),
    
    /**
     * 失敗 - 決済エラー
     */
    FAILED("失敗"),
    
    /**
     * 返金済み - 全額または一部返金
     */
    REFUNDED("返金済み"),
    
    /**
     * キャンセル - 決済キャンセル
     */
    CANCELLED("キャンセル");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 終端ステータスかどうかを判定
     * 終端ステータスのトランザクションは状態変更不可
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED || this == CANCELLED;
    }

    /**
     * 返金可能かどうかを判定
     */
    public boolean isRefundable() {
        return this == COMPLETED;
    }
}
