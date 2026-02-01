package com.fintech.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS設定
 * 
 * 【設計思想】
 * - フロントエンドとバックエンドの完全分離を実現
 * - セキュアなCORS設定
 * - 環境別の設定対応
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 許可するオリジン
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",      // React開発サーバー
                "http://localhost:5173",      // Vite開発サーバー
                "https://payment-dashboard.vercel.app"  // 本番フロントエンド
        ));
        
        // 許可するHTTPメソッド
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 許可するヘッダー
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Request-Id",
                "X-Idempotency-Key"
        ));
        
        // 認証情報の送信許可
        config.setAllowCredentials(true);
        
        // プリフライトリクエストのキャッシュ時間
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        
        return new CorsFilter(source);
    }
}
