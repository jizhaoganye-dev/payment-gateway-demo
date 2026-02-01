package com.fintech.payment.mapper;

import com.fintech.payment.dto.PaymentRequest;
import com.fintech.payment.dto.PaymentResponse;
import com.fintech.payment.entity.Transaction;
import org.mapstruct.*;

/**
 * トランザクション Entity ⇔ DTO マッパー
 * 
 * 【設計思想】
 * - MapStructによるコンパイル時マッピングコード生成
 * - 手書きの詰め替えコードを排除し、保守性向上
 * - 型安全なマッピングを保証
 * 
 * 【金融システムにおける重要性】
 * - Entity（DB層）とDTO（API層）を明確に分離
 * - 内部データ構造の外部漏洩を防止
 * - APIバージョニングへの柔軟な対応が可能
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TransactionMapper {

    /**
     * PaymentRequest → Transaction Entity 変換
     * 新規トランザクション作成時に使用
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "errorCode", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(PaymentRequest request);

    /**
     * Transaction Entity → PaymentResponse DTO 変換
     * API レスポンス生成時に使用
     */
    PaymentResponse toResponse(Transaction entity);

    /**
     * Entity の部分更新
     * 既存Entityに対してリクエスト内容を適用
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(PaymentRequest request, @MappingTarget Transaction entity);
}
