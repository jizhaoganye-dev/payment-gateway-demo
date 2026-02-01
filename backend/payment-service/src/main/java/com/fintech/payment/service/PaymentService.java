package com.fintech.payment.service;

import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.dto.PaymentResponse;
import com.fintech.payment.entity.Transaction;
import com.fintech.payment.entity.TransactionStatus;
import com.fintech.payment.exception.InsufficientFundsException;
import com.fintech.payment.exception.InvalidCardException;
import com.fintech.payment.exception.PaymentException;
import com.fintech.payment.exception.PaymentTimeoutException;
import com.fintech.payment.exception.TransactionNotFoundException;
import com.fintech.payment.mapper.TransactionMapper;
import com.fintech.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 決済サービス
 * 
 * 【アーキテクチャ設計】
 * - Controller → Service → Repository の3層アーキテクチャ
 * - EntityはService層内で完結、外部へはDTOのみを公開
 * - MapStructによる型安全なEntity⇔DTO変換
 * 
 * 【トランザクション設計】
 * - ACID特性の完全保証
 * - 分離レベル: READ_COMMITTED（金融標準）
 * - 冪等性キーによる二重処理防止
 * 
 * 【エラーハンドリング】
 * - 業務例外と技術例外の明確な分離
 * - 全エラーにエラーコードを付与
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final IdempotencyService idempotencyService;
    
    private final Random random = new Random();

    /**
     * 決済処理実行
     * 
     * 【処理フロー】
     * 1. 冪等性チェック（キャッシュがあれば即座に返却）
     * 2. トランザクション作成（PENDING状態）
     * 3. 決済プロセッサー呼び出し（外部API）
     * 4. 結果に応じてステータス更新
     * 5. 冪等性キーにレスポンスをキャッシュ
     * 
     * @param request 決済リクエスト
     * @param idempotencyKey 冪等性キー（オプション）
     * @return 決済レスポンス
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
        log.info("決済処理開始: merchantId={}, amount={}, currency={}, idempotencyKey={}", 
                request.getMerchantId(), request.getAmount(), request.getCurrency(), 
                idempotencyKey != null ? idempotencyKey.substring(0, 8) + "..." : "null");

        // 冪等性チェック
        Optional<PaymentResponse> cachedResponse = idempotencyService.checkAndLock(idempotencyKey, request);
        if (cachedResponse.isPresent()) {
            log.info("冪等性キーからキャッシュ返却");
            return cachedResponse.get();
        }

        Transaction transaction = null;
        try {
            // Entity作成（MapStructを使用）
            transaction = createTransaction(request);
            
            // 決済処理実行
            transaction = executePayment(transaction);
            
            // レスポンス生成（MapStructを使用）
            PaymentResponse response = transactionMapper.toResponse(transaction);
            
            // 冪等性キーにキャッシュ
            idempotencyService.markCompleted(idempotencyKey, response);
            
            log.info("決済処理成功: transactionId={}", transaction.getTransactionId());
            return response;
            
        } catch (Exception e) {
            log.error("決済処理失敗: error={}", e.getMessage());
            
            // トランザクションがある場合は失敗ステータスに更新
            if (transaction != null && transaction.getId() != null) {
                markTransactionFailed(transaction, e);
            }
            
            // 冪等性キーを失敗状態に
            idempotencyService.markFailed(idempotencyKey);
            
            throw e;
        }
    }

    /**
     * トランザクション取得
     */
    @Transactional(readOnly = true)
    public PaymentResponse getTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return transactionMapper.toResponse(transaction);
    }

    /**
     * 加盟店のトランザクション一覧取得
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getTransactionsByMerchant(String merchantId, Pageable pageable) {
        return transactionRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable)
                .map(transactionMapper::toResponse);
    }

    /**
     * 返金処理
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public PaymentResponse refundTransaction(String transactionId, BigDecimal refundAmount) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        validateRefundRequest(transaction, refundAmount);

        log.info("返金処理開始: transactionId={}, refundAmount={}", transactionId, refundAmount);

        // 返金処理（実際は外部APIを呼び出す）
        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction.setProcessedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        log.info("返金処理完了: transactionId={}", transactionId);
        return transactionMapper.toResponse(transaction);
    }

    /**
     * 売上サマリー取得
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesSummary(String merchantId) {
        BigDecimal totalSales = transactionRepository.getTotalSalesByMerchant(merchantId);
        var statusCounts = transactionRepository.countByStatus();

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

    /**
     * トランザクションEntity作成
     */
    private Transaction createTransaction(PaymentRequest request) {
        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setStatus(TransactionStatus.PENDING);
        return transactionRepository.save(transaction);
    }

    /**
     * 決済処理実行
     * 
     * 実際のプロダクションでは、ここで外部決済ゲートウェイ
     * （Stripe, PayPal, GMO等）のAPIを呼び出す
     */
    private Transaction executePayment(Transaction transaction) {
        // ステータスを処理中に更新
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction = transactionRepository.save(transaction);

        // 外部API呼び出しシミュレーション
        simulateExternalPaymentProcessor(transaction);

        // 完了ステータスに更新
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setProcessedAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    /**
     * 外部決済プロセッサーシミュレーション
     * 
     * 実際のテストケース用に、特定の条件でエラーを発生させる
     */
    private void simulateExternalPaymentProcessor(Transaction transaction) {
        // 処理時間シミュレーション
        try {
            Thread.sleep(100 + random.nextInt(200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // テスト用: 特定の顧客IDでエラーを発生
        String customerId = transaction.getCustomerId();
        
        if (customerId != null) {
            if (customerId.contains("INSUFFICIENT")) {
                throw new InsufficientFundsException("残高が不足しています");
            }
            if (customerId.contains("INVALID_CARD")) {
                throw new InvalidCardException("カード情報が無効です");
            }
            if (customerId.contains("TIMEOUT")) {
                throw new PaymentTimeoutException("決済処理がタイムアウトしました");
            }
        }

        // 確率的なエラー（1%）- 実際の障害をシミュレート
        if (random.nextDouble() < 0.01) {
            throw new PaymentException("PROCESSOR_ERROR", "決済プロセッサーでエラーが発生しました");
        }
    }

    /**
     * トランザクション失敗処理
     */
    private void markTransactionFailed(Transaction transaction, Exception e) {
        try {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setProcessedAt(LocalDateTime.now());
            
            if (e instanceof PaymentException pe) {
                transaction.setErrorCode(pe.getErrorCode());
                transaction.setErrorMessage(pe.getMessage());
            } else {
                transaction.setErrorCode("UNKNOWN_ERROR");
                transaction.setErrorMessage(e.getMessage());
            }
            
            transactionRepository.save(transaction);
        } catch (Exception ex) {
            log.error("トランザクション失敗更新エラー: {}", ex.getMessage());
        }
    }

    /**
     * 返金リクエストバリデーション
     */
    private void validateRefundRequest(Transaction transaction, BigDecimal refundAmount) {
        if (!transaction.getStatus().isRefundable()) {
            throw new PaymentException("REFUND_NOT_ALLOWED", 
                    "このトランザクションは返金できません。ステータス: " + transaction.getStatus());
        }

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("INVALID_REFUND_AMOUNT", 
                    "返金額は0より大きい必要があります");
        }

        if (refundAmount.compareTo(transaction.getAmount()) > 0) {
            throw new PaymentException("INVALID_REFUND_AMOUNT", 
                    "返金額が元の金額を超えています");
        }
    }
}
