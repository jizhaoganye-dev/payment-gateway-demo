package com.fintech.payment.controller;

import com.fintech.payment.dto.ApiResponse;
import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.dto.PaymentResponse;
import com.fintech.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 決済APIコントローラー
 * 
 * 【API設計思想】
 * - RESTful設計原則の遵守
 * - OpenAPI (Swagger) による完全なドキュメント化
 * - 統一されたレスポンス形式
 * 
 * 【リファクタリング履歴】
 * BEFORE (モノリス時代):
 *   - フロントエンドとバックエンドが同一プロジェクト
 *   - サーバーサイドレンダリング
 *   - APIドキュメントなし
 * 
 * AFTER (マイクロサービス化):
 *   - 完全分離されたREST API
 *   - OpenAPI 3.0によるドキュメント自動生成
 *   - フロントエンドとの疎結合化
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment API", description = "決済処理API - 金融グレードのトランザクション管理")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 決済処理実行
     */
    @Operation(
            summary = "決済処理実行",
            description = "新規決済トランザクションを作成し、決済処理を実行します。冪等性キーを指定することで重複処理を防止できます。"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "決済処理成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "リクエストパラメータ不正",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "サーバーエラー",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        log.info("決済リクエスト受信: merchantId={}, requestId={}", request.getMerchantId(), requestId);
        
        PaymentResponse response = paymentService.processPayment(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "決済処理が完了しました"));
    }

    /**
     * トランザクション取得
     */
    @Operation(
            summary = "トランザクション取得",
            description = "トランザクションIDを指定して、決済トランザクションの詳細を取得します。"
    )
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getTransaction(
            @Parameter(description = "トランザクションID", example = "txn_abc123")
            @PathVariable String transactionId
    ) {
        PaymentResponse response = paymentService.getTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 加盟店のトランザクション一覧取得
     */
    @Operation(
            summary = "加盟店トランザクション一覧",
            description = "指定した加盟店の決済トランザクション一覧を取得します。ページネーション対応。"
    )
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getTransactionsByMerchant(
            @Parameter(description = "加盟店ID", example = "MERCHANT_001")
            @PathVariable String merchantId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<PaymentResponse> transactions = paymentService.getTransactionsByMerchant(merchantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    /**
     * 返金処理
     */
    @Operation(
            summary = "返金処理",
            description = "完了済みトランザクションに対して返金処理を実行します。全額または一部返金に対応。"
    )
    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundTransaction(
            @Parameter(description = "トランザクションID", example = "txn_abc123")
            @PathVariable String transactionId,
            @Parameter(description = "返金額", example = "5000.00")
            @RequestParam BigDecimal amount
    ) {
        log.info("返金リクエスト受信: transactionId={}, amount={}", transactionId, amount);
        
        PaymentResponse response = paymentService.refundTransaction(transactionId, amount);
        
        return ResponseEntity.ok(ApiResponse.success(response, "返金処理が完了しました"));
    }

    /**
     * 売上サマリー取得
     */
    @Operation(
            summary = "売上サマリー取得",
            description = "指定した加盟店の売上サマリーを取得します。ダッシュボード表示用。"
    )
    @GetMapping("/merchant/{merchantId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesSummary(
            @Parameter(description = "加盟店ID", example = "MERCHANT_001")
            @PathVariable String merchantId
    ) {
        Map<String, Object> summary = paymentService.getSalesSummary(merchantId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * ヘルスチェック
     */
    @Operation(summary = "ヘルスチェック", description = "サービスの稼働状況を確認します。")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "UP",
                "service", "payment-service",
                "version", "1.0.0"
        )));
    }
}
