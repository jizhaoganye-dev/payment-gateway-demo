package com.fintech.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 決済ゲートウェイサービス - メインアプリケーション
 * 
 * 【アーキテクチャ特徴】
 * - マイクロサービス設計: 決済処理を独立したサービスとして分離
 * - 疎結合: REST APIによるフロントエンドとの完全分離
 * - スケーラビリティ: 水平スケーリング対応
 * 
 * 【リファクタリング履歴】
 * v0.x: モノリシック構造 - 全機能が単一アプリに結合
 * v1.0: マイクロサービス化 - 決済処理を独立サービスとして切り出し
 *       - 認証サービス（別リポジトリ）
 *       - 通知サービス（別リポジトリ）
 *       - 決済サービス（本サービス）
 * 
 * @author AI Native Engineer
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
