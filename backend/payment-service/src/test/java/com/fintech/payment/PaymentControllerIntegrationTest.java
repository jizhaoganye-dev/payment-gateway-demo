package com.fintech.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.entity.PaymentMethod;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PaymentController 統合テスト
 * 
 * 【テスト設計】
 * - 実際のHTTPリクエスト/レスポンスをシミュレート
 * - Spring Security、バリデーション、例外ハンドリングを含むE2Eテスト
 * - 冪等性キーの動作確認
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PaymentController 統合テスト")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createdTransactionId;

    // =========================================================================
    // ヘルスチェックテスト
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("ヘルスチェック正常")
    void healthCheck_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/v1/payments/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("payment-service"));
    }

    // =========================================================================
    // 決済処理テスト
    // =========================================================================
    @Nested
    @DisplayName("決済処理 POST /api/v1/payments")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ProcessPaymentTests {

        @Test
        @Order(1)
        @DisplayName("正常: 有効なリクエストで決済成功")
        void processPayment_withValidRequest_shouldReturn201() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("10000.00"))
                    .currency("JPY")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .merchantId("MERCHANT_001")
                    .customerId("CUSTOMER_001")
                    .description("テスト決済")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("X-Idempotency-Key", UUID.randomUUID().toString()))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.transactionId").exists())
                    .andExpect(jsonPath("$.data.amount").value(10000.00))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andReturn();

            // 後のテスト用にトランザクションIDを保存
            String responseBody = result.getResponse().getContentAsString();
            createdTransactionId = objectMapper.readTree(responseBody)
                    .path("data").path("transactionId").asText();
        }

        @Test
        @Order(2)
        @DisplayName("異常: 金額がマイナスの場合はバリデーションエラー")
        void processPayment_withNegativeAmount_shouldReturn400() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("-100.00"))
                    .currency("JPY")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .merchantId("MERCHANT_001")
                    .customerId("CUSTOMER_001")
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors.amount").exists());
        }

        @Test
        @Order(3)
        @DisplayName("異常: 必須項目欠落時はバリデーションエラー")
        void processPayment_withMissingFields_shouldReturn400() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    // merchantId, customerId, currency が欠落
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
        }

        @Test
        @Order(4)
        @DisplayName("異常: 通貨コードが不正な場合はバリデーションエラー")
        void processPayment_withInvalidCurrency_shouldReturn400() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("INVALID")  // 3文字でない
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .merchantId("MERCHANT_001")
                    .customerId("CUSTOMER_001")
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors.currency").exists());
        }

        @Test
        @Order(5)
        @DisplayName("異常: 残高不足のシミュレーション")
        void processPayment_withInsufficientFunds_shouldReturn402() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("10000.00"))
                    .currency("JPY")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .merchantId("MERCHANT_001")
                    .customerId("INSUFFICIENT_FUNDS_TEST")  // 特定キーワードでエラー発生
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isPaymentRequired())
                    .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_FUNDS"));
        }

        @Test
        @Order(6)
        @DisplayName("異常: 無効なカード情報のシミュレーション")
        void processPayment_withInvalidCard_shouldReturn400() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("10000.00"))
                    .currency("JPY")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .merchantId("MERCHANT_001")
                    .customerId("INVALID_CARD_TEST")  // 特定キーワードでエラー発生
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CARD"));
        }

        @Test
        @Order(7)
        @DisplayName("異常: タイムアウトのシミュレーション")
        void processPayment_withTimeout_shouldReturn503() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("10000.00"))
                    .currency("JPY")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .merchantId("MERCHANT_001")
                    .customerId("TIMEOUT_TEST")  // 特定キーワードでエラー発生
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.errorCode").value("PAYMENT_TIMEOUT"));
        }
    }

    // =========================================================================
    // トランザクション取得テスト
    // =========================================================================
    @Nested
    @DisplayName("トランザクション取得 GET /api/v1/payments/{transactionId}")
    class GetTransactionTests {

        @Test
        @DisplayName("正常: 存在するトランザクションIDで取得成功")
        void getTransaction_withValidId_shouldReturn200() throws Exception {
            // まず決済を作成
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("5000.00"))
                    .currency("JPY")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .merchantId("MERCHANT_001")
                    .customerId("CUSTOMER_001")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String transactionId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("transactionId").asText();

            // 取得
            mockMvc.perform(get("/api/v1/payments/" + transactionId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.transactionId").value(transactionId));
        }

        @Test
        @DisplayName("異常: 存在しないトランザクションIDで404")
        void getTransaction_withInvalidId_shouldReturn404() throws Exception {
            mockMvc.perform(get("/api/v1/payments/non_existent_id"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("TRANSACTION_NOT_FOUND"));
        }
    }

    // =========================================================================
    // 返金処理テスト
    // =========================================================================
    @Nested
    @DisplayName("返金処理 POST /api/v1/payments/{transactionId}/refund")
    class RefundTests {

        @Test
        @DisplayName("正常: 完了済みトランザクションの返金成功")
        void refundTransaction_withCompletedTransaction_shouldReturn200() throws Exception {
            // まず決済を作成
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("10000.00"))
                    .currency("JPY")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .merchantId("MERCHANT_001")
                    .customerId("CUSTOMER_001")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String transactionId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("transactionId").asText();

            // 返金
            mockMvc.perform(post("/api/v1/payments/" + transactionId + "/refund")
                            .param("amount", "5000.00"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));
        }

        @Test
        @DisplayName("異常: 返金額が元金額を超える場合は400")
        void refundTransaction_withExcessAmount_shouldReturn400() throws Exception {
            // まず決済を作成
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("1000.00"))
                    .currency("JPY")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .merchantId("MERCHANT_001")
                    .customerId("CUSTOMER_001")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String transactionId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("transactionId").asText();

            // 返金（金額超過）
            mockMvc.perform(post("/api/v1/payments/" + transactionId + "/refund")
                            .param("amount", "5000.00"))  // 元金額1000を超える
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_REFUND_AMOUNT"));
        }
    }

    // =========================================================================
    // 冪等性キーテスト
    // =========================================================================
    @Nested
    @DisplayName("冪等性キー X-Idempotency-Key")
    class IdempotencyTests {

        @Test
        @DisplayName("同一冪等性キーで同一レスポンスを返却")
        void processPayment_withSameIdempotencyKey_shouldReturnSameResponse() throws Exception {
            String idempotencyKey = UUID.randomUUID().toString();
            
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("7500.00"))
                    .currency("JPY")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .merchantId("MERCHANT_001")
                    .customerId("CUSTOMER_001")
                    .build();

            // 1回目のリクエスト
            MvcResult firstResult = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("X-Idempotency-Key", idempotencyKey))
                    .andExpect(status().isCreated())
                    .andReturn();

            String firstTransactionId = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                    .path("data").path("transactionId").asText();

            // 2回目のリクエスト（同一キー）
            MvcResult secondResult = mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("X-Idempotency-Key", idempotencyKey))
                    .andExpect(status().isCreated())
                    .andReturn();

            String secondTransactionId = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                    .path("data").path("transactionId").asText();

            // 同一トランザクションIDが返却される
            org.assertj.core.api.Assertions.assertThat(secondTransactionId)
                    .isEqualTo(firstTransactionId);
        }
    }

    // =========================================================================
    // HTTPエラーテスト
    // =========================================================================
    @Nested
    @DisplayName("HTTPエラーレスポンス")
    class HttpErrorTests {

        @Test
        @DisplayName("405: 許可されていないHTTPメソッド")
        void invalidMethod_shouldReturn405() throws Exception {
            mockMvc.perform(delete("/api/v1/payments"))
                    .andDo(print())
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("415: サポートされていないContent-Type")
        void invalidContentType_shouldReturn415() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("invalid content"))
                    .andDo(print())
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
        }

        @Test
        @DisplayName("400: 不正なJSON形式")
        void invalidJson_shouldReturn400() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"));
        }
    }
}
