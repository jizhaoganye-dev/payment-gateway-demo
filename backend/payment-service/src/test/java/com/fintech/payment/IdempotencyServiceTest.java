package com.fintech.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.dto.PaymentResponse;
import com.fintech.payment.entity.IdempotencyKey;
import com.fintech.payment.entity.IdempotencyKey.IdempotencyStatus;
import com.fintech.payment.entity.PaymentMethod;
import com.fintech.payment.entity.TransactionStatus;
import com.fintech.payment.exception.IdempotencyConflictException;
import com.fintech.payment.repository.IdempotencyKeyRepository;
import com.fintech.payment.service.IdempotencyService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * IdempotencyService 単体テスト
 * 
 * 冪等性キー管理の正確性を検証
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService 単体テスト")
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IdempotencyService idempotencyService;

    private PaymentRequest validRequest;
    private PaymentResponse mockResponse;

    @BeforeEach
    void setUp() {
        validRequest = PaymentRequest.builder()
                .amount(new BigDecimal("10000.00"))
                .currency("JPY")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .merchantId("MERCHANT_001")
                .customerId("CUSTOMER_001")
                .build();

        mockResponse = PaymentResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .amount(validRequest.getAmount())
                .currency(validRequest.getCurrency())
                .status(TransactionStatus.COMPLETED)
                .build();
    }

    @Nested
    @DisplayName("checkAndLock - 新規キー")
    class CheckAndLockNewKeyTests {

        @Test
        @DisplayName("新規キーの場合、空のOptionalを返し、キーを登録")
        void checkAndLock_withNewKey_shouldReturnEmptyAndRegister() {
            // Given
            String key = UUID.randomUUID().toString();
            when(idempotencyKeyRepository.findByIdempotencyKey(key))
                    .thenReturn(Optional.empty());
            when(idempotencyKeyRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<PaymentResponse> result = idempotencyService.checkAndLock(key, validRequest);

            // Then
            assertThat(result).isEmpty();
            
            ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
            verify(idempotencyKeyRepository).save(captor.capture());
            
            IdempotencyKey savedKey = captor.getValue();
            assertThat(savedKey.getIdempotencyKey()).isEqualTo(key);
            assertThat(savedKey.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
        }

        @Test
        @DisplayName("nullキーの場合、何もせず空のOptionalを返す")
        void checkAndLock_withNullKey_shouldReturnEmpty() {
            // When
            Optional<PaymentResponse> result = idempotencyService.checkAndLock(null, validRequest);

            // Then
            assertThat(result).isEmpty();
            verify(idempotencyKeyRepository, never()).findByIdempotencyKey(any());
        }

        @Test
        @DisplayName("空白キーの場合、何もせず空のOptionalを返す")
        void checkAndLock_withBlankKey_shouldReturnEmpty() {
            // When
            Optional<PaymentResponse> result = idempotencyService.checkAndLock("   ", validRequest);

            // Then
            assertThat(result).isEmpty();
            verify(idempotencyKeyRepository, never()).findByIdempotencyKey(any());
        }
    }

    @Nested
    @DisplayName("checkAndLock - 既存キー（完了）")
    class CheckAndLockCompletedKeyTests {

        @Test
        @DisplayName("完了済みキーの場合、キャッシュされたレスポンスを返す")
        void checkAndLock_withCompletedKey_shouldReturnCachedResponse() throws Exception {
            // Given
            String key = UUID.randomUUID().toString();
            String cachedJson = objectMapper.writeValueAsString(mockResponse);
            
            IdempotencyKey existingKey = IdempotencyKey.builder()
                    .idempotencyKey(key)
                    .status(IdempotencyStatus.COMPLETED)
                    .cachedResponse(cachedJson)
                    .requestHash(computeHash(validRequest))
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            
            when(idempotencyKeyRepository.findByIdempotencyKey(key))
                    .thenReturn(Optional.of(existingKey));

            // When
            Optional<PaymentResponse> result = idempotencyService.checkAndLock(key, validRequest);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTransactionId()).isEqualTo(mockResponse.getTransactionId());
            verify(idempotencyKeyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("checkAndLock - 既存キー（処理中）")
    class CheckAndLockProcessingKeyTests {

        @Test
        @DisplayName("処理中キーの場合、IdempotencyConflictExceptionをスロー")
        void checkAndLock_withProcessingKey_shouldThrowConflict() {
            // Given
            String key = UUID.randomUUID().toString();
            
            IdempotencyKey existingKey = IdempotencyKey.builder()
                    .idempotencyKey(key)
                    .status(IdempotencyStatus.PROCESSING)
                    .requestHash(computeHash(validRequest))
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            
            when(idempotencyKeyRepository.findByIdempotencyKey(key))
                    .thenReturn(Optional.of(existingKey));

            // When & Then
            assertThatThrownBy(() -> idempotencyService.checkAndLock(key, validRequest))
                    .isInstanceOf(IdempotencyConflictException.class)
                    .hasMessageContaining("処理中");
        }
    }

    @Nested
    @DisplayName("checkAndLock - リクエスト不整合")
    class CheckAndLockRequestMismatchTests {

        @Test
        @DisplayName("同一キーで異なるリクエストの場合、IdempotencyConflictExceptionをスロー")
        void checkAndLock_withDifferentRequest_shouldThrowConflict() {
            // Given
            String key = UUID.randomUUID().toString();
            
            IdempotencyKey existingKey = IdempotencyKey.builder()
                    .idempotencyKey(key)
                    .status(IdempotencyStatus.COMPLETED)
                    .requestHash("different_hash")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            
            when(idempotencyKeyRepository.findByIdempotencyKey(key))
                    .thenReturn(Optional.of(existingKey));

            // When & Then
            assertThatThrownBy(() -> idempotencyService.checkAndLock(key, validRequest))
                    .isInstanceOf(IdempotencyConflictException.class)
                    .hasMessageContaining("異なるリクエスト");
        }
    }

    @Nested
    @DisplayName("checkAndLock - 期限切れキー")
    class CheckAndLockExpiredKeyTests {

        @Test
        @DisplayName("期限切れキーの場合、新規キーとして扱う")
        void checkAndLock_withExpiredKey_shouldTreatAsNew() {
            // Given
            String key = UUID.randomUUID().toString();
            
            IdempotencyKey existingKey = IdempotencyKey.builder()
                    .idempotencyKey(key)
                    .status(IdempotencyStatus.COMPLETED)
                    .requestHash(computeHash(validRequest))
                    .expiresAt(LocalDateTime.now().minusHours(1))  // 期限切れ
                    .build();
            
            when(idempotencyKeyRepository.findByIdempotencyKey(key))
                    .thenReturn(Optional.of(existingKey));
            when(idempotencyKeyRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<PaymentResponse> result = idempotencyService.checkAndLock(key, validRequest);

            // Then
            assertThat(result).isEmpty();
            verify(idempotencyKeyRepository).delete(existingKey);
            verify(idempotencyKeyRepository).save(any());
        }
    }

    @Nested
    @DisplayName("markCompleted")
    class MarkCompletedTests {

        @Test
        @DisplayName("キーを完了状態に更新しレスポンスをキャッシュ")
        void markCompleted_shouldUpdateStatusAndCacheResponse() {
            // Given
            String key = UUID.randomUUID().toString();
            
            IdempotencyKey existingKey = IdempotencyKey.builder()
                    .idempotencyKey(key)
                    .status(IdempotencyStatus.PROCESSING)
                    .build();
            
            when(idempotencyKeyRepository.findByIdempotencyKey(key))
                    .thenReturn(Optional.of(existingKey));
            when(idempotencyKeyRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            idempotencyService.markCompleted(key, mockResponse);

            // Then
            ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
            verify(idempotencyKeyRepository).save(captor.capture());
            
            IdempotencyKey savedKey = captor.getValue();
            assertThat(savedKey.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
            assertThat(savedKey.getCachedResponse()).isNotNull();
            assertThat(savedKey.getTransactionId()).isEqualTo(mockResponse.getTransactionId());
        }

        @Test
        @DisplayName("nullキーの場合、何もしない")
        void markCompleted_withNullKey_shouldDoNothing() {
            // When
            idempotencyService.markCompleted(null, mockResponse);

            // Then
            verify(idempotencyKeyRepository, never()).findByIdempotencyKey(any());
        }
    }

    @Nested
    @DisplayName("markFailed")
    class MarkFailedTests {

        @Test
        @DisplayName("キーを失敗状態に更新")
        void markFailed_shouldUpdateStatusToFailed() {
            // Given
            String key = UUID.randomUUID().toString();
            
            IdempotencyKey existingKey = IdempotencyKey.builder()
                    .idempotencyKey(key)
                    .status(IdempotencyStatus.PROCESSING)
                    .build();
            
            when(idempotencyKeyRepository.findByIdempotencyKey(key))
                    .thenReturn(Optional.of(existingKey));
            when(idempotencyKeyRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            idempotencyService.markFailed(key);

            // Then
            ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
            verify(idempotencyKeyRepository).save(captor.capture());
            
            assertThat(captor.getValue().getStatus()).isEqualTo(IdempotencyStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("cleanupExpiredKeys")
    class CleanupExpiredKeysTests {

        @Test
        @DisplayName("期限切れキーを削除")
        void cleanupExpiredKeys_shouldDeleteExpired() {
            // Given
            when(idempotencyKeyRepository.deleteExpiredKeys(any()))
                    .thenReturn(5);

            // When
            int deleted = idempotencyService.cleanupExpiredKeys();

            // Then
            assertThat(deleted).isEqualTo(5);
            verify(idempotencyKeyRepository).deleteExpiredKeys(any());
        }
    }

    // Helper method
    private String computeHash(PaymentRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
