package com.fintech.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.entity.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PaymentController 統合テスト
 * 
 * 【テスト設計思想】
 * - E2E（エンドツーエンド）テスト
 * - 実際のHTTPリクエスト・レスポンスを検証
 * - データベース連携を含む統合テスト
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PaymentController 統合テスト")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/payments - 決済処理成功")
    void processPayment_ShouldReturn201() throws Exception {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .currency("JPY")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .merchantId("MERCHANT_001")
                .customerId("CUSTOMER_001")
                .description("統合テスト決済")
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.amount").value(5000.00))
                .andReturn();

        System.out.println("Response: " + result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("POST /api/v1/payments - バリデーションエラー（金額なし）")
    void processPayment_WithoutAmount_ShouldReturn400() throws Exception {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .currency("JPY")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .merchantId("MERCHANT_001")
                .customerId("CUSTOMER_001")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - バリデーションエラー（金額が負）")
    void processPayment_WithNegativeAmount_ShouldReturn400() throws Exception {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("-100.00"))
                .currency("JPY")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .merchantId("MERCHANT_001")
                .customerId("CUSTOMER_001")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - 存在しないトランザクション")
    void getTransaction_WhenNotExists_ShouldReturn404() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/payments/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TRANSACTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/health - ヘルスチェック")
    void healthCheck_ShouldReturnUp() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/payments/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("payment-service"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/merchant/{id} - 加盟店トランザクション一覧")
    void getTransactionsByMerchant_ShouldReturnPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/payments/merchant/MERCHANT_001")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
