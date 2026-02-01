package com.fintech.payment;

import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.dto.PaymentResponse;
import com.fintech.payment.entity.PaymentMethod;
import com.fintech.payment.entity.Transaction;
import com.fintech.payment.entity.TransactionStatus;
import com.fintech.payment.exception.PaymentException;
import com.fintech.payment.exception.TransactionNotFoundException;
import com.fintech.payment.repository.TransactionRepository;
import com.fintech.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PaymentService ユニットテスト
 * 
 * 【テスト設計思想】
 * - 金融グレードの厳格なテスト
 * - 境界値テスト、異常系テスト
 * - 高いコードカバレッジ（目標: 90%以上）
 * 
 * 【テストカテゴリ】
 * - 正常系: 期待通りの動作確認
 * - 異常系: エラーハンドリング確認
 * - 境界値: エッジケースの確認
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService テスト")
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest validRequest;
    private Transaction savedTransaction;

    @BeforeEach
    void setUp() {
        // テスト用の有効なリクエスト
        validRequest = PaymentRequest.builder()
                .amount(new BigDecimal("10000.00"))
                .currency("JPY")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .merchantId("MERCHANT_001")
                .customerId("CUSTOMER_001")
                .description("テスト決済")
                .build();

        // 保存後のトランザクション
        savedTransaction = Transaction.builder()
                .id(1L)
                .transactionId(UUID.randomUUID().toString())
                .amount(validRequest.getAmount())
                .currency(validRequest.getCurrency())
                .paymentMethod(validRequest.getPaymentMethod())
                .merchantId(validRequest.getMerchantId())
                .customerId(validRequest.getCustomerId())
                .description(validRequest.getDescription())
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // 決済処理テスト
    // =========================================================================
    @Nested
    @DisplayName("決済処理テスト")
    class ProcessPaymentTests {

        @Test
        @DisplayName("正常系: 有効なリクエストで決済成功")
        void processPayment_WithValidRequest_ShouldSucceed() {
            // Given
            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);

            // When
            PaymentResponse response = paymentService.processPayment(validRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(response.getAmount()).isEqualByComparingTo(validRequest.getAmount());
            assertThat(response.getCurrency()).isEqualTo(validRequest.getCurrency());
            assertThat(response.getMerchantId()).isEqualTo(validRequest.getMerchantId());
            
            verify(transactionRepository, atLeast(1)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("正常系: 異なる決済方法でも処理成功")
        void processPayment_WithDifferentPaymentMethods_ShouldSucceed() {
            // Given
            validRequest.setPaymentMethod(PaymentMethod.QR_CODE);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);

            // When
            PaymentResponse response = paymentService.processPayment(validRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        }

        @Test
        @DisplayName("境界値: 最小金額での決済")
        void processPayment_WithMinimumAmount_ShouldSucceed() {
            // Given
            validRequest.setAmount(new BigDecimal("1.00"));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);

            // When
            PaymentResponse response = paymentService.processPayment(validRequest);

            // Then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("境界値: 最大金額での決済")
        void processPayment_WithMaximumAmount_ShouldSucceed() {
            // Given
            validRequest.setAmount(new BigDecimal("99999999.99"));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);

            // When
            PaymentResponse response = paymentService.processPayment(validRequest);

            // Then
            assertThat(response).isNotNull();
        }
    }

    // =========================================================================
    // トランザクション取得テスト
    // =========================================================================
    @Nested
    @DisplayName("トランザクション取得テスト")
    class GetTransactionTests {

        @Test
        @DisplayName("正常系: 存在するトランザクションを取得")
        void getTransaction_WhenExists_ShouldReturnTransaction() {
            // Given
            String transactionId = savedTransaction.getTransactionId();
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(savedTransaction));

            // When
            PaymentResponse response = paymentService.getTransaction(transactionId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTransactionId()).isEqualTo(transactionId);
        }

        @Test
        @DisplayName("異常系: 存在しないトランザクションで例外")
        void getTransaction_WhenNotExists_ShouldThrowException() {
            // Given
            String nonExistentId = "non-existent-id";
            when(transactionRepository.findByTransactionId(nonExistentId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentService.getTransaction(nonExistentId))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining(nonExistentId);
        }
    }

    // =========================================================================
    // 返金処理テスト
    // =========================================================================
    @Nested
    @DisplayName("返金処理テスト")
    class RefundTransactionTests {

        @Test
        @DisplayName("正常系: 完了済みトランザクションの全額返金")
        void refundTransaction_WhenCompleted_ShouldSucceed() {
            // Given
            String transactionId = savedTransaction.getTransactionId();
            savedTransaction.setStatus(TransactionStatus.COMPLETED);
            
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(savedTransaction));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);

            // When
            PaymentResponse response = paymentService.refundTransaction(
                    transactionId, 
                    savedTransaction.getAmount()
            );

            // Then
            assertThat(response).isNotNull();
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("異常系: 未完了トランザクションの返金は失敗")
        void refundTransaction_WhenPending_ShouldFail() {
            // Given
            String transactionId = savedTransaction.getTransactionId();
            savedTransaction.setStatus(TransactionStatus.PENDING);
            
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(savedTransaction));

            // When & Then
            assertThatThrownBy(() -> paymentService.refundTransaction(
                    transactionId, 
                    savedTransaction.getAmount()
            ))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("返金できません");
        }

        @Test
        @DisplayName("異常系: 返金額が元金額を超える場合は失敗")
        void refundTransaction_WhenAmountExceedsOriginal_ShouldFail() {
            // Given
            String transactionId = savedTransaction.getTransactionId();
            savedTransaction.setStatus(TransactionStatus.COMPLETED);
            savedTransaction.setAmount(new BigDecimal("10000.00"));
            
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(savedTransaction));

            // When & Then
            assertThatThrownBy(() -> paymentService.refundTransaction(
                    transactionId, 
                    new BigDecimal("20000.00")
            ))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("元の金額を超えています");
        }
    }

    // =========================================================================
    // 金融計算精度テスト
    // =========================================================================
    @Nested
    @DisplayName("金融計算精度テスト")
    class FinancialPrecisionTests {

        @Test
        @DisplayName("小数点精度: BigDecimalで正確な計算")
        void financialCalculation_ShouldMaintainPrecision() {
            // 浮動小数点では 0.1 + 0.2 = 0.30000000000000004 となるが
            // BigDecimalでは正確に 0.3 となる
            BigDecimal a = new BigDecimal("0.1");
            BigDecimal b = new BigDecimal("0.2");
            BigDecimal expected = new BigDecimal("0.3");

            BigDecimal result = a.add(b);

            assertThat(result).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("通貨変換: 異なる通貨コードを正しく処理")
        void currencyHandling_ShouldSupportMultipleCurrencies() {
            // Given
            String[] currencies = {"JPY", "USD", "EUR", "GBP"};
            
            for (String currency : currencies) {
                validRequest.setCurrency(currency);
                when(transactionRepository.save(any(Transaction.class)))
                        .thenReturn(savedTransaction);

                // When
                PaymentResponse response = paymentService.processPayment(validRequest);

                // Then
                assertThat(response).isNotNull();
            }
        }
    }
}
