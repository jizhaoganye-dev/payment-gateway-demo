package com.fintech.payment.service;

import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.dto.PaymentResponse;
import com.fintech.payment.entity.Transaction;
import com.fintech.payment.entity.TransactionStatus;
import com.fintech.payment.exception.PaymentException;
import com.fintech.payment.exception.TransactionNotFoundException;
import com.fintech.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 決済サービス
 * 
 * 【設計思想】
 * - トランザクション境界の明確化
 * - 金融グレードのエラーハンドリング
 * - 監査ログの自動記録
 * 
 * 【リファクタリング履歴】
 * BEFORE (モノリス時代):
 *   - 決済ロジック、通知、ログが混在
 *   - 巨大なメソッド（500行超）
 *   - テスト困難
 * 
 * AFTER (マイクロサービス化):
 *   - 単一責任: 決済処理のみに集中
 *   - 小さなメソッド（各30行以内）
 *   - 高いテストカバレッジ
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final TransactionRepository transactionRepository;

    /**
     * 決済処理実行
     * 
     * @param request 決済リクエスト
     * @return 決済レスポンス
     * @throws PaymentException 決済処理エラー
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("決済処理開始: merchantId={}, amount={}, currency={}", 
                request.getMerchantId(), request.getAmount(), request.getCurrency());

        // トランザクション作成
        Transaction transaction = createTransaction(request);
        
        try {
            // 決済処理（実際はここで外部決済プロセッサーを呼び出す）
            transaction = executePayment(transaction);
            log.info("決済処理成功: transactionId={}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("決済処理失敗: transactionId={}, error={}", 
                    transaction.getTransactionId(), e.getMessage());
            transaction = markAsFailed(transaction, "PAYMENT_ERROR", e.getMessage());
        }

        return toResponse(transaction);
    }

    /**
     * トランザクション取得
     */
    @Transactional(readOnly = true)
    public PaymentResponse getTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return toResponse(transaction);
    }

    /**
     * 加盟店のトランザクション一覧取得
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getTransactionsByMerchant(String merchantId, Pageable pageable) {
        return transactionRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable)
                .map(this::toResponse);
    }

    /**
     * 返金処理
     */
    public PaymentResponse refundTransaction(String transactionId, BigDecimal refundAmount) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (!transaction.getStatus().isRefundable()) {
            throw new PaymentException("REFUND_NOT_ALLOWED", 
                    "このトランザクションは返金できません。ステータス: " + transaction.getStatus());
        }

        if (refundAmount.compareTo(transaction.getAmount()) > 0) {
            throw new PaymentException("INVALID_REFUND_AMOUNT", 
                    "返金額が元の金額を超えています");
        }

        log.info("返金処理開始: transactionId={}, refundAmount={}", transactionId, refundAmount);

        // 返金処理（実際はここで外部決済プロセッサーを呼び出す）
        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction.setProcessedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        log.info("返金処理完了: transactionId={}", transactionId);
        return toResponse(transaction);
    }

    /**
     * 売上サマリー取得
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesSummary(String merchantId) {
        BigDecimal totalSales = transactionRepository.getTotalSalesByMerchant(merchantId);
        List<Object[]> statusCounts = transactionRepository.countByStatus();

        Map<String, Long> statusMap = statusCounts.stream()
                .collect(Collectors.toMap(
                        arr -> ((TransactionStatus) arr[0]).name(),
                        arr -> (Long) arr[1]
                ));

        return Map.of(
                "merchantId", merchantId,
                "totalSales", totalSales,
                "transactionsByStatus", statusMap,
                "generatedAt", LocalDateTime.now()
        );
    }

    // ========== Private Methods ==========

    private Transaction createTransaction(PaymentRequest request) {
        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .merchantId(request.getMerchantId())
                .customerId(request.getCustomerId())
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .status(TransactionStatus.PENDING)
                .build();

        return transactionRepository.save(transaction);
    }

    private Transaction executePayment(Transaction transaction) {
        // ステータスを処理中に更新
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction = transactionRepository.save(transaction);

        // 決済処理シミュレーション（実際は外部APIを呼び出す）
        simulatePaymentProcessing();

        // 完了ステータスに更新
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setProcessedAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    private Transaction markAsFailed(Transaction transaction, String errorCode, String errorMessage) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setErrorCode(errorCode);
        transaction.setErrorMessage(errorMessage);
        transaction.setProcessedAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    private void simulatePaymentProcessing() {
        // 実際の決済処理をシミュレート
        try {
            Thread.sleep(100); // 100ms の処理時間
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private PaymentResponse toResponse(Transaction transaction) {
        return PaymentResponse.builder()
                .transactionId(transaction.getTransactionId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .paymentMethod(transaction.getPaymentMethod())
                .merchantId(transaction.getMerchantId())
                .customerId(transaction.getCustomerId())
                .description(transaction.getDescription())
                .errorCode(transaction.getErrorCode())
                .errorMessage(transaction.getErrorMessage())
                .createdAt(transaction.getCreatedAt())
                .processedAt(transaction.getProcessedAt())
                .build();
    }
}
