package com.fintech.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 設定
 * 
 * 【設計思想】
 * - プロフェッショナルなAPIドキュメント自動生成
 * - 開発者体験の向上
 * - フロントエンド開発者との円滑なコミュニケーション
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Gateway API")
                        .version("1.0.0")
                        .description("""
                                ## 🏦 金融グレード決済ゲートウェイAPI
                                
                                ### 概要
                                本APIは、大手金融機関向けの決済基盤モダン化プロジェクトで使用される
                                マイクロサービスベースの決済処理APIです。
                                
                                ### 主な機能
                                - **決済処理**: クレジットカード、銀行振込、QRコード決済等に対応
                                - **トランザクション管理**: 決済履歴の照会、ステータス管理
                                - **返金処理**: 全額・一部返金に対応
                                - **売上レポート**: ダッシュボード向けの集計API
                                
                                ### 技術スタック
                                - Spring Boot 3.2
                                - Java 17
                                - PostgreSQL / H2
                                - Docker対応
                                
                                ### 開発手法
                                - AIツール（Cursor/Claude）を活用した高速開発
                                - テスト駆動開発（TDD）
                                - クリーンアーキテクチャ
                                """)
                        .contact(new Contact()
                                .name("AI Native Engineer")
                                .email("contact@example.com")
                                .url("https://github.com/jizhaoganye-dev"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("開発環境"),
                        new Server()
                                .url("https://api.payment-gateway.example.com")
                                .description("本番環境")
                ));
    }
}
