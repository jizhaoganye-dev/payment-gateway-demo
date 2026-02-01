package com.fintech.payment.entity;

/**
 * 決済方法
 * 
 * 【設計思想】
 * 各決済方法に対応するプロセッサーを抽象化し、
 * Strategyパターンで実装を切り替え可能にする
 * 
 * 【リファクタリング履歴】
 * BEFORE: if-else チェーンで決済方法を分岐
 * AFTER: Enum + Strategyパターンで拡張性向上
 */
public enum PaymentMethod {
    
    /**
     * クレジットカード決済
     */
    CREDIT_CARD("クレジットカード", true),
    
    /**
     * デビットカード決済
     */
    DEBIT_CARD("デビットカード", true),
    
    /**
     * 銀行振込
     */
    BANK_TRANSFER("銀行振込", false),
    
    /**
     * コンビニ決済
     */
    CONVENIENCE_STORE("コンビニ決済", false),
    
    /**
     * QRコード決済
     */
    QR_CODE("QRコード決済", true),
    
    /**
     * 電子マネー
     */
    E_MONEY("電子マネー", true),
    
    /**
     * ウォレット決済
     */
    WALLET("ウォレット", true);

    private final String displayName;
    private final boolean instantCapture;

    PaymentMethod(String displayName, boolean instantCapture) {
        this.displayName = displayName;
        this.instantCapture = instantCapture;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 即時決済かどうか
     * true: リアルタイムで決済完了
     * false: 非同期処理（銀行振込など）
     */
    public boolean isInstantCapture() {
        return instantCapture;
    }
}
