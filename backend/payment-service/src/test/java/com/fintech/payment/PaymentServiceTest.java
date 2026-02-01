package com.fintech.payment;

import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.dto.PaymentResponse;
import com.fintech.payment.entity.PaymentMethod;
import com.fintech.payment.entity.Transaction;
import com.fintech.payment.entity.TransactionStatus;
import com.fintech.payment.exception.*;
import com.fintech.payment.mapper.TransactionMapper;
import com.fintech.payment.repository.TransactionRepository;
import com.fintech.payment.service.IdempotencyService;
import com.fintech.payment.service.PaymentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PaymentService 単体テスト
 * 
 * 【テスト設計】
 * - カバレッジ目標: 80%以上
 * - 正常系・異常系・境界値を網羅
 * - 金融システム特有のエラーパターンをテスト
 * 
 * 【テストカテゴリ】
 * 1. 正常系テスト
 * 2. 異常系テスト（残高不足、無効カード、タイムアウト）
 * 3. 境界値テスト
 * 4. 冪等性テスト
 * 5. トランザクション管理テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 単体テスト")
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest validRequest;
    private Transaction mockTransaction;
    private PaymentResponse mockResponse;

    @BeforeEach
    void setUp() {
        validRequest = PaymentRequest.builder()
                .amount(new BigDecimal("10000.00"))
                .currency("JPY")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .merchantId("MERCHANT_001")
                .customerId("CUSTOMER_001")
                .description("テスト決済")
                .build();

        mockTransaction = Transaction.builder()
                .id(1L)
                .transactionId(UUID.randomUUID().toString())
                .amount(validRequest.getAmount())
                .currency(validRequest.getCurrency())
                .paymentMethod(validRequest.getPaymentMethod())
                .merchantId(validRequest.getMerchantId())
                .customerId(validRequest.getCustomerId())
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        mockResponse = PaymentResponse.builder()
                .transactionId(mockTransaction.getTransactionId())
                .amount(mockTransaction.getAmount())
                .currency(mockTransaction.getCurrency())
                .status(mockTransaction.getStatus())
                .paymentMethod(mockTransaction.getPaymentMethod())
                .merchantId(mockTransaction.getMerchantId())
                .customerId(mockTransaction.getCustomerId())
                .createdAt(mockTransaction.getCreatedAt())
                .processedAt(mockTransaction.getProcessedAt())
                .build();
    }

    // =========================================================================
    // 正常系テスト
    // =========================================================================
    @Nested
    @DisplayName("正常系: 決済処理成功")
    class SuccessfulPaymentTests {

        @Test
        @DisplayName("有効なリクエストで決済成功")
        void processPayment_withValidRequest_shouldSucceed() {
            // Given
            when(idempotencyService.checkAndLock(any(), any())).thenReturn(Optional.empty());
            when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
            when(transactionRepository.save(any())).thenReturn(mockTransaction);
            when(transactionMapper.toResponse(any())).thenReturn(mockResponse);

            // When
            PaymentResponse response = paymentService.processPayment(validRequest, null);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(response.getAmount()).isEqualByComparingTo(validRequest.getAmount());
            
            verify(transactionRepository, atLeast(2)).save(any(Transaction.class));
            verify(transactionMapper).toResponse(any(Transaction.class));
        }

        @ParameterizedTest
        @EnumSource(PaymentMethod.class)
        @DisplayName("全ての決済方法で処理成功")
        void processPayment_withAllPaymentMethods_shouldSucceed(PaymentMethod method) {
            // Given
            validRequest.setPaymentMethod(method);
            when(idempotencyService.checkAndLock(any(), any())).thenReturn(Optional.empty());
            when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
            when(transactionRepository.save(any())).thenReturn(mockTransaction);
            when(transactionMapper.toResponse(any())).thenReturn(mockResponse);

            // When
            PaymentResponse response = paymentService.processPayment(validRequest, null);

            // Then
            assertThat(response).isNotNull();
        }

        @ParameterizedTest
        @CsvSource({
            "JPY, 1.00",
            "JPY, 99999999.99",
            "USD, 0.01",
            "EUR, 50000.00"
        })
        @DisplayName("様々な通貨と金額で処理成功")
        void processPayment_withVariousCurrencyAndAmount_shouldSucceed(String currency, String amount) {
            // Given
            validRequest.setCurrency(currency);
            validRequest.setAmount(new BigDecimal(amount));
            when(idempotencyService.checkAndLock(any(), any())).thenReturn(Optional.empty());
            when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
            when(transactionRepository.save(any())).thenReturn(mockTransaction);
            when(transactionMapper.toResponse(any())).thenReturn(mockResponse);

            // When
            PaymentResponse response = paymentService.processPayment(validRequest, null);

            // Then
            assertThat(response).isNotNull();
        }
    }

    // =========================================================================
    // 異常系テスト: 残高不足
    // =========================================================================
    @Nested
    @DisplayName("異常系: 残高不足")
    class InsufficientFundsTests {

        @Test
        @DisplayName("残高不足時にInsufficientFundsExceptionがスローされる")
        void processPayment_withInsufficientFunds_shouldThrowException() {
            // Given
            validRequest.setCustomerId("INSUFFICIENT_FUNDS_TEST");
            mockTransaction.setCustomerId("INSUFFICIENT_FUNDS_TEST");
            
            when(idempotencyService.checkAndLock(any(), any())).thenReturn(Optional.empty());
            when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
            when(transactionRepository.save(any())).thenReturn(mockTransaction);

            // When & Then
            assertThatThrownBy(() -> paymentService.processPayment(validRequest, null))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("残高");
            
            verify(idempotencyService).markFailed(any());
        }
    }

    // =========================================================================
    // 異常系テスト: 無効なカード情報
    // =========================================================================
    @Nested
    @DisplayName("異常系: 無効なカード情報")
    class InvalidCardTests {

        @Test
        @DisplayName("無効なカード情報でInvalidCardExceptionがスローされる")
        void processPayment_withInvalidCard_shouldThrowException() {
            // Given
            validRequest.setCustomerId("INVALID_CARD_TEST");
            mockTransaction.setCustomerId("INVALID_CARD_TEST");
            
            when(idempotencyService.checkAndLock(any(), any())).thenReturn(Optional.empty());
            when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
            when(transactionRepository.save(any())).thenReturn(mockTransaction);

            // When & Then
            assertThatThrownBy(() -> paymentService.processPayment(validRequest, null))
                    .isInstanceOf(InvalidCardException.class)
                    .hasMessageContaining("カード");
        }
    }

    // =========================================================================
    // 異常系テスト: タイムアウト
    // =========================================================================
    @Nested
    @DisplayName("異常系: タイムアウト")
    class TimeoutTests {

        @Test
        @DisplayName("タイムアウト時にPaymentTimeoutExceptionがスローされる")
        void processPayment_withTimeout_shouldThrowException() {
            // Given
            validRequest.setCustomerId("TIMEOUT_TEST");
            mockTransaction.setCustomerId("TIMEOUT_TEST");
            
            when(idempotencyService.checkAndLock(any(), any())).thenReturn(Optional.empty());
            when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
            when(transactionRepository.save(any())).thenReturn(mockTransaction);

            // When & Then
            assertThatThrownBy(() -> paymentService.processPayment(validRequest, null))
                    .isInstanceOf(PaymentTimeoutException.class)
                    .hasMessageContaining("タイムアウト");
        }
    }

    // =========================================================================
    // 異常系テスト: トランザクション未検出
    // =========================================================================
    @Nested
    @DisplayName("異常系: トランザクション未検出")
    class TransactionNotFoundTests {

        @Test
        @DisplayName("存在しないトランザクションIDで例外がスローされる")
        void getTransaction_withNonExistentId_shouldThrowException() {
            // Given
            String nonExistentId = "non_existent_id";
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
    class RefundTests {

        @Test
        @DisplayName("完了済みトランザクションの返金成功")
        void refundTransaction_withCompletedTransaction_shouldSucceed() {
            // Given
            mockTransaction.setStatus(TransactionStatus.COMPLETED);
            when(transactionRepository.findByTransactionId(any()))
                    .thenReturn(Optional.of(mockTransaction));
            when(transactionRepository.save(any())).thenReturn(mockTransaction);
            when(transactionMapper.toResponse(any())).thenReturn(mockResponse);

            // When
            PaymentResponse response = paymentService.refundTransaction(
                    mockTransaction.getTransactionId(),
                    new BigDecimal("5000.00")
            );

            // Then
            assertThat(response).isNotNull();
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("返金額が元金額を超える場合は失敗")
        void refundTransaction_withExcessAmount_shouldFail() {
            // Given
            mockTransaction.setStatus(TransactionStatus.COMPLETED);
            mockTransaction.setAmount(new BigDecimal("10000.00"));
            when(transactionRepository.findByTransactionId(any()))
                    .thenReturn(Optional.of(mockTransaction));

            // When & Then
            assertThatThrownBy(() -> paymentService.refundTransaction(
                    mockTransaction.getTransactionId(),
                    new BigDecimal("20000.00")
            ))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("超えています");
        }

        @Test
        @DisplayName("未完了トランザクションの返金は失敗")
        void refundTransaction_withPendingTransaction_shouldFail() {
            // Given
            mockTransaction.setStatus(TransactionStatus.PENDING);
            when(transactionRepository.findByTransactionId(any()))
                    .thenReturn(Optional.of(mockTransaction));

            // When & Then
            assertThatThrownBy(() -> paymentService.refundTransaction(
                    mockTransaction.getTransactionId(),
                    new BigDecimal("5000.00")
            ))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("返金できません");
        }

        @Test
        @DisplayName("マイナス金額の返金は失敗")
        void refundTransaction_withNegativeAmount_shouldFail() {
            // Given
            mockTransaction.setStatus(TransactionStatus.COMPLETED);
            when(transactionRepository.findByTransactionId(any()))
                    .thenReturn(Optional.of(mockTransaction));

            // When & Then
            assertThatThrownBy(() -> paymentService.refundTransaction(
                    mockTransaction.getTransactionId(),
                    new BigDecimal("-1000.00")
            ))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("0より大きい");
        }
    }

    // =========================================================================
    // 冪等性テスト
    // =========================================================================
    @Nested
    @DisplayName("冪等性テスト")
    class IdempotencyTests {

        @Test
        @DisplayName("キャッシュ済みレスポンスがある場合、即座に返却")
        void processPayment_withCachedResponse_shouldReturnCached() {
            // Given
            String idempotencyKey = UUID.randomUUID().toString();
            when(idempotencyService.checkAndLock(eq(idempotencyKey), any()))
                    .thenReturn(Optional.of(mockResponse));

            // When
            PaymentResponse response = paymentService.processPayment(validRequest, idempotencyKey);

            // Then
            assertThat(response).isEqualTo(mockResponse);
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("新規キーの場合、処理を実行")
        void processPayment_withNewKey_shouldProcess() {
            // Given
            String idempotencyKey = UUID.randomUUID().toString();
            when(idempotencyService.checkAndLock(eq(idempotencyKey), any()))
                    .thenReturn(Optional.empty());
            when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
            when(transactionRepository.save(any())).thenReturn(mockTransaction);
            when(transactionMapper.toResponse(any())).thenReturn(mockResponse);

            // When
            PaymentResponse response = paymentService.processPayment(validRequest, idempotencyKey);

            // Then
            assertThat(response).isNotNull();
            verify(idempotencyService).markCompleted(eq(idempotencyKey), any());
        }
    }

    // =========================================================================
    // 境界値テスト
    // =========================================================================
    @Nested
    @DisplayName("境界値テスト")
    class BoundaryTests {

        @Test
        @DisplayName("最小金額（1円）で処理成功")
        void processPayment_withMinimumAmount_shouldSucceed() {
            // Given
            validRequest.setAmount(new BigDecimal("1.00"));
            when(idempotencyService.checkAndLock(any(), any())).thenReturn(Optional.empty());
            when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
            when(transactionRepository.save(any())).thenReturn(mockTransaction);
            when(transactionMapper.toResponse(any())).thenReturn(mockResponse);

            // When
            PaymentResponse response = paymentService.processPayment(validRequest, null);

            // Then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("最大金額で処理成功")
        void processPayment_withMaximumAmount_shouldSucceed() {
            // Given
            validRequest.setAmount(new BigDecimal("99999999.99"));
            when(idempotencyService.checkAndLock(any(), any())).thenReturn(Optional.empty());
            when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
            when(transactionRepository.save(any())).thenReturn(mockTransaction);
            when(transactionMapper.toResponse(any())).thenReturn(mockResponse);

            // When
            PaymentResponse response = paymentService.processPayment(validRequest, null);

            // Then
            assertThat(response).isNotNull();
        }
    }

    // =========================================================================
    // 金融計算精度テスト
    // =========================================================================
    @Nested
    @DisplayName("金融計算精度テスト")
    class FinancialPrecisionTests {

        @Test
        @DisplayName("BigDecimalで正確な計算が行われる")
        void financialCalculation_shouldMaintainPrecision() {
            // 浮動小数点では 0.1 + 0.2 = 0.30000000000000004
            // BigDecimalでは正確に 0.3
            BigDecimal a = new BigDecimal("0.1");
            BigDecimal b = new BigDecimal("0.2");
            BigDecimal expected = new BigDecimal("0.3");

            assertThat(a.add(b)).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("小数点以下4桁の精度を維持")
        void financialCalculation_shouldMaintain4DecimalPrecision() {
            BigDecimal amount = new BigDecimal("12345.6789");
            BigDecimal rate = new BigDecimal("0.0825");
            BigDecimal expected = new BigDecimal("1018.5185");

            BigDecimal result = amount.multiply(rate).setScale(4, java.math.RoundingMode.HALF_UP);

            assertThat(result).isEqualByComparingTo(expected);
        }
    }
}
