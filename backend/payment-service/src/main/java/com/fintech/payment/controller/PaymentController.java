package com.fintech.payment.controller;

import com.fintech.payment.dto.ApiResponse;
import com.fintech.payment.dto.ErrorResponse;
import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.dto.PaymentResponse;
import com.fintech.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
 * 【アーキテクチャ設計】
 * - Controllerはリクエストの受付・レスポンスの返却のみを担当
 * - 全てのビジネスロジックはService層に委譲
 * - DTOを介した疎結合なAPI設計
 * 
 * 【OpenAPI仕様】
 * - 全エンドポイントに @Operation アノテーション
 * - レスポンス例、エラーパターンを完全記述
 * - Swagger UIで仕様書として機能
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment API", description = "金融グレード決済処理API - 冪等性保証、ACID準拠")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 決済処理実行
     */
    @Operation(
        summary = "決済処理実行",
        description = """
            新規決済トランザクションを作成し、決済処理を実行します。
            
            ## 冪等性保証
            `X-Idempotency-Key` ヘッダーを指定することで、ネットワーク障害時のリトライでも
            二重決済を防止できます。同一キーでの再リクエストは、キャッシュされたレスポンスを返却します。
            
            ## 対応決済方法
            - CREDIT_CARD: クレジットカード
            - DEBIT_CARD: デビットカード
            - BANK_TRANSFER: 銀行振込
            - QR_CODE: QRコード決済
            - E_MONEY: 電子マネー
            - WALLET: ウォレット
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "決済処理成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "transactionId": "txn_a1b2c3d4",
                        "amount": 10000.00,
                        "currency": "JPY",
                        "status": "COMPLETED",
                        "paymentMethod": "CREDIT_CARD",
                        "merchantId": "MERCHANT_001",
                        "customerId": "CUSTOMER_001",
                        "createdAt": "2026-02-01T10:30:00",
                        "processedAt": "2026-02-01T10:30:01"
                      },
                      "message": "決済処理が完了しました",
                      "timestamp": "2026-02-01T10:30:01"
                    }
                    """)
            ),
            headers = @Header(
                name = "X-Request-Id",
                description = "リクエストトレーシング用ID",
                schema = @Schema(type = "string", example = "req_abc123")
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "バリデーションエラー / 不正なリクエスト",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "errorCode": "VALIDATION_ERROR",
                      "message": "入力値が不正です",
                      "status": 400,
                      "path": "/api/v1/payments",
                      "requestId": "req_abc123",
                      "fieldErrors": {
                        "amount": "金額は1以上である必要があります",
                        "currency": "通貨コードは3文字である必要があります"
                      },
                      "timestamp": "2026-02-01T10:30:00"
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "402",
            description = "残高不足",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "冪等性キー衝突（同一キーで処理中）",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "503",
            description = "決済処理タイムアウト",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Parameter(description = "決済リクエスト", required = true)
            @Valid @RequestBody PaymentRequest request,
            
            @Parameter(
                description = "冪等性キー（UUID推奨）。同一キーでのリトライは同一結果を返却",
                example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            
            @Parameter(description = "リクエストトレーシング用ID", example = "req_abc123")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        log.info("決済リクエスト受信: merchantId={}, amount={}, requestId={}", 
                request.getMerchantId(), request.getAmount(), requestId);
        
        PaymentResponse response = paymentService.processPayment(request, idempotencyKey);
        
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
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "取得成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "トランザクション未検出",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getTransaction(
            @Parameter(description = "トランザクションID", example = "txn_a1b2c3d4", required = true)
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
        description = """
            指定した加盟店の決済トランザクション一覧を取得します。
            ページネーション対応（デフォルト20件/ページ）。
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "取得成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getTransactionsByMerchant(
            @Parameter(description = "加盟店ID", example = "MERCHANT_001", required = true)
            @PathVariable String merchantId,
            
            @Parameter(description = "ページネーション設定")
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
        description = """
            完了済みトランザクションに対して返金処理を実行します。
            全額または一部返金に対応。
            
            ## 制約
            - COMPLETED ステータスのトランザクションのみ返金可能
            - 返金額は元の金額以下である必要があります
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "返金成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "返金不可（ステータス不正 / 金額超過）",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "トランザクション未検出",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundTransaction(
            @Parameter(description = "トランザクションID", example = "txn_a1b2c3d4", required = true)
            @PathVariable String transactionId,
            
            @Parameter(description = "返金額", example = "5000.00", required = true)
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
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "取得成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "merchantId": "MERCHANT_001",
                        "totalSales": 1234567.00,
                        "transactionsByStatus": {
                          "COMPLETED": 245,
                          "FAILED": 5,
                          "REFUNDED": 3
                        },
                        "generatedAt": "2026-02-01T10:30:00"
                      },
                      "timestamp": "2026-02-01T10:30:00"
                    }
                    """)
            )
        )
    })
    @GetMapping("/merchant/{merchantId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesSummary(
            @Parameter(description = "加盟店ID", example = "MERCHANT_001", required = true)
            @PathVariable String merchantId
    ) {
        Map<String, Object> summary = paymentService.getSalesSummary(merchantId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * ヘルスチェック
     */
    @Operation(
        summary = "ヘルスチェック",
        description = "サービスの稼働状況を確認します。ロードバランサー、監視システム向け。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "サービス正常稼働",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "status": "UP",
                        "service": "payment-service",
                        "version": "1.0.0"
                      },
                      "timestamp": "2026-02-01T10:30:00"
                    }
                    """)
            )
        )
    })
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "UP",
                "service", "payment-service",
                "version", "1.0.0"
        )));
    }
}
