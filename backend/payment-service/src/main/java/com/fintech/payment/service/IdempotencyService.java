package com.fintech.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.dto.PaymentResponse;
import com.fintech.payment.entity.IdempotencyKey;
import com.fintech.payment.entity.IdempotencyKey.IdempotencyStatus;
import com.fintech.payment.exception.IdempotencyConflictException;
import com.fintech.payment.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 冪等性キー管理サービス
 * 
 * 【設計思想】
 * 金融取引における二重処理を防止するための冪等性保証サービス
 * 
 * 【処理フロー】
 * 1. リクエスト受信時: キーの存在確認
 * 2. 新規キー: PROCESSING状態で登録し、処理を続行
 * 3. 既存キー（処理中）: 競合エラー（409 Conflict）
 * 4. 既存キー（完了済）: キャッシュされたレスポンスを返却
 * 
 * 【トランザクション設計】
 * REQUIRES_NEW を使用し、メイントランザクションから独立
 * → 決済処理が失敗しても、冪等性キーの状態は正しく管理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;
    
    /** 冪等性キーの有効期間（24時間） */
    private static final int KEY_EXPIRY_HOURS = 24;

    /**
     * 冪等性キーをチェックし、既存のレスポンスがあれば返却
     * 
     * @param key 冪等性キー
     * @param request リクエスト内容（ハッシュ比較用）
     * @return キャッシュされたレスポンス（存在する場合）
     * @throws IdempotencyConflictException キーが処理中の場合
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<PaymentResponse> checkAndLock(String key, PaymentRequest request) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        String requestHash = computeHash(request);
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByIdempotencyKey(key);

        if (existingKey.isPresent()) {
            IdempotencyKey idempKey = existingKey.get();

            // 期限切れの場合は新規として扱う
            if (idempKey.isExpired()) {
                log.info("冪等性キー期限切れ: key={}", key);
                idempotencyKeyRepository.delete(idempKey);
                return createNewKey(key, requestHash);
            }

            // リクエスト内容の整合性チェック
            if (!requestHash.equals(idempKey.getRequestHash())) {
                log.warn("冪等性キー衝突: 同一キーで異なるリクエスト内容 key={}", key);
                throw new IdempotencyConflictException(
                    "同一の冪等性キーで異なるリクエスト内容が送信されました"
                );
            }

            // 処理中の場合
            if (idempKey.getStatus() == IdempotencyStatus.PROCESSING) {
                log.warn("冪等性キー処理中: key={}", key);
                throw new IdempotencyConflictException(
                    "このリクエストは現在処理中です。しばらく待ってから再試行してください"
                );
            }

            // 完了済みの場合、キャッシュを返却
            if (idempKey.isCompleted() && idempKey.getCachedResponse() != null) {
                log.info("冪等性キーからキャッシュ返却: key={}", key);
                return Optional.of(deserializeResponse(idempKey.getCachedResponse()));
            }
        }

        return createNewKey(key, requestHash);
    }

    /**
     * 処理完了時にレスポンスをキャッシュ
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String key, PaymentResponse response) {
        if (key == null || key.isBlank()) {
            return;
        }

        idempotencyKeyRepository.findByIdempotencyKey(key).ifPresent(idempKey -> {
            idempKey.setStatus(IdempotencyStatus.COMPLETED);
            idempKey.setTransactionId(response.getTransactionId());
            idempKey.setCachedResponse(serializeResponse(response));
            idempotencyKeyRepository.save(idempKey);
            log.info("冪等性キー完了: key={}, transactionId={}", key, response.getTransactionId());
        });
    }

    /**
     * 処理失敗時にステータスを更新
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        idempotencyKeyRepository.findByIdempotencyKey(key).ifPresent(idempKey -> {
            idempKey.setStatus(IdempotencyStatus.FAILED);
            idempotencyKeyRepository.save(idempKey);
            log.info("冪等性キー失敗: key={}", key);
        });
    }

    /**
     * 期限切れキーのクリーンアップ
     */
    @Transactional
    public int cleanupExpiredKeys() {
        int deleted = idempotencyKeyRepository.deleteExpiredKeys(LocalDateTime.now());
        if (deleted > 0) {
            log.info("期限切れ冪等性キーを削除: count={}", deleted);
        }
        return deleted;
    }

    // ========== Private Methods ==========

    private Optional<PaymentResponse> createNewKey(String key, String requestHash) {
        IdempotencyKey newKey = IdempotencyKey.builder()
                .idempotencyKey(key)
                .status(IdempotencyStatus.PROCESSING)
                .requestHash(requestHash)
                .expiresAt(LocalDateTime.now().plusHours(KEY_EXPIRY_HOURS))
                .build();
        idempotencyKeyRepository.save(newKey);
        log.info("冪等性キー新規登録: key={}", key);
        return Optional.empty();
    }

    private String computeHash(PaymentRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.error("リクエストハッシュ計算エラー", e);
            return "";
        }
    }

    private String serializeResponse(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("レスポンスシリアライズエラー", e);
            return null;
        }
    }

    private PaymentResponse deserializeResponse(String json) {
        try {
            return objectMapper.readValue(json, PaymentResponse.class);
        } catch (JsonProcessingException e) {
            log.error("レスポンスデシリアライズエラー", e);
            throw new RuntimeException("キャッシュされたレスポンスの読み取りに失敗しました", e);
        }
    }
}
