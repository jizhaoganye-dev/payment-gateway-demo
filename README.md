# 金融決済ゲートウェイ・デモシステム

金融グレードの決済処理APIを実装したデモシステムです。マイクロサービス移行を想定した設計で、エンタープライズレベルの品質基準を満たしています。

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![React](https://img.shields.io/badge/React-18-blue)
![TypeScript](https://img.shields.io/badge/TypeScript-5-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 目次

1. [プロジェクト概要](#プロジェクト概要)
2. [技術スタック](#技術スタック)
3. [アーキテクチャ設計](#アーキテクチャ設計)
4. [セキュリティ対策](#セキュリティ対策)
5. [API仕様](#api仕様)
6. [テスト戦略](#テスト戦略)
7. [セットアップ手順](#セットアップ手順)
8. [デプロイメント](#デプロイメント)
9. [技術的選定理由](#技術的選定理由)

---

## プロジェクト概要

本プロジェクトは、大手金融機関の決済基盤モダン化プロジェクトを想定し、以下の技術要件を満たすデモシステムです。

### 主要機能

- **決済処理API**: クレジットカード、デビットカード、QRコード、銀行振込等の決済処理
- **冪等性保証**: `X-Idempotency-Key` ヘッダーによる二重処理防止
- **リアルタイムダッシュボード**: React/TypeScriptによるモダンなUI
- **包括的なテスト**: 単体テスト・統合テストによる80%以上のカバレッジ

### 対象ユースケース

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Frontend  │─────▶│  API Gateway │─────▶│  Payment    │
│  Dashboard  │      │             │      │  Service    │
└─────────────┘      └─────────────┘      └─────────────┘
                                                 │
                                                 ▼
                                          ┌─────────────┐
                                          │  Database   │
                                          │ (H2/PostgreSQL)│
                                          └─────────────┘
```

---

## 技術スタック

### バックエンド

| 技術 | バージョン | 用途 |
|------|-----------|------|
| Java | 17 LTS | メイン言語 |
| Spring Boot | 3.2 | アプリケーションフレームワーク |
| Spring Data JPA | 3.2 | データアクセス層 |
| MapStruct | 1.5.5 | Entity/DTOマッピング |
| Lombok | 1.18 | ボイラープレート削減 |
| H2 Database | 2.2 | 開発用インメモリDB |
| PostgreSQL | 15 | 本番用RDBMS |
| SpringDoc OpenAPI | 2.3 | API仕様書自動生成 |

### フロントエンド

| 技術 | バージョン | 用途 |
|------|-----------|------|
| React | 18 | UIライブラリ |
| TypeScript | 5 | 型安全なJS |
| Vite | 5 | ビルドツール |
| TailwindCSS | 3 | CSSフレームワーク |
| TanStack Query | 5 | データフェッチング |
| Recharts | 2 | データ可視化 |

### インフラ

| 技術 | 用途 |
|------|------|
| Docker | コンテナ化 |
| Docker Compose | ローカル開発環境 |
| Nginx | フロントエンド配信・リバースプロキシ |
| GitHub Actions | CI/CD (想定) |

---

## アーキテクチャ設計

### レイヤードアーキテクチャ

```
┌─────────────────────────────────────────────────────────────┐
│                     Controller Layer                        │
│  - リクエストの受付・バリデーション                          │
│  - DTOによる入出力                                          │
│  - OpenAPIアノテーション                                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                          │
│  - ビジネスロジックの集約                                    │
│  - トランザクション管理 (@Transactional)                     │
│  - 冪等性キー管理                                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                         │
│  - Spring Data JPAによるデータアクセス                       │
│  - カスタムクエリ                                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Entity Layer                           │
│  - JPAエンティティ                                          │
│  - 金融グレードの精度 (BigDecimal)                          │
└─────────────────────────────────────────────────────────────┘
```

### マイクロサービス移行設計

本プロジェクトは、モノリスからマイクロサービスへの移行を想定した設計です。

#### 分離可能なドメイン境界

1. **Payment Service** (本プロジェクト)
   - 決済処理のコア機能
   - トランザクション管理
   - 冪等性保証

2. **Notification Service** (将来分離)
   - 決済結果通知
   - メール/SMS配信

3. **Reporting Service** (将来分離)
   - 売上レポート生成
   - データ分析

4. **Fraud Detection Service** (将来分離)
   - 不正検知
   - リスクスコアリング

#### サービス間通信設計

```yaml
# 同期通信
- REST API (OpenAPI準拠)
- gRPC (高パフォーマンス要件)

# 非同期通信 (将来実装)
- Apache Kafka (イベント駆動)
- Amazon SQS (AWS環境)
```

---

## セキュリティ対策

### 実装済みセキュリティ機能

| 対策 | 実装内容 |
|------|---------|
| **入力バリデーション** | jakarta.validation による厳格な検証 |
| **SQLインジェクション防止** | Spring Data JPA のパラメータバインディング |
| **XSS対策** | React の自動エスケープ |
| **CORS設定** | 許可オリジンの明示的指定 |
| **エラー情報秘匿** | 本番環境でのスタックトレース非表示 |
| **冪等性キー** | 二重処理・リプレイ攻撃防止 |

### 金融システム固有のセキュリティ

```java
// 金融データの精度保証
@Column(precision = 19, scale = 4)
private BigDecimal amount;  // 浮動小数点は使用しない

// 冪等性キーによる二重決済防止
@Transactional(isolation = Isolation.READ_COMMITTED)
public PaymentResponse processPayment(
    PaymentRequest request, 
    String idempotencyKey
) {
    // キーの存在確認 → 処理 → レスポンスキャッシュ
}
```

### 本番環境での追加対策 (推奨)

- HTTPS/TLS 1.3 の強制
- WAF (Web Application Firewall) 導入
- レートリミット実装
- API キー / OAuth 2.0 認証
- PCI DSS 準拠のトークナイゼーション

---

## API仕様

### エンドポイント一覧

| メソッド | エンドポイント | 説明 |
|---------|---------------|------|
| POST | `/api/v1/payments` | 決済処理実行 |
| GET | `/api/v1/payments/{transactionId}` | トランザクション取得 |
| GET | `/api/v1/payments/merchant/{merchantId}` | 加盟店別一覧取得 |
| POST | `/api/v1/payments/{transactionId}/refund` | 返金処理 |
| GET | `/api/v1/payments/merchant/{merchantId}/summary` | 売上サマリー |
| GET | `/api/v1/payments/health` | ヘルスチェック |

### リクエスト/レスポンス例

#### 決済処理

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "amount": 10000.00,
    "currency": "JPY",
    "paymentMethod": "CREDIT_CARD",
    "merchantId": "MERCHANT_001",
    "customerId": "CUSTOMER_001",
    "description": "商品購入"
  }'
```

#### 成功レスポンス

```json
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
```

#### エラーレスポンス

```json
{
  "errorCode": "INSUFFICIENT_FUNDS",
  "message": "残高が不足しています",
  "status": 402,
  "path": "/api/v1/payments",
  "requestId": "req_abc123",
  "timestamp": "2026-02-01T10:30:00"
}
```

### Swagger UI

開発環境では `http://localhost:8080/swagger-ui.html` でAPI仕様書を確認できます。

---

## テスト戦略

### テストピラミッド

```
        ╱╲
       ╱  ╲         E2E Tests (少)
      ╱────╲        - ブラウザ自動化
     ╱      ╲
    ╱────────╲      Integration Tests (中)
   ╱          ╲     - API統合テスト
  ╱────────────╲    - DB連携テスト
 ╱              ╲
╱────────────────╲  Unit Tests (多)
                    - Service層テスト
                    - 異常系シナリオ
```

### テストカバレッジ目標: 80%以上

#### 単体テスト (JUnit 5 + Mockito)

```java
@Test
@DisplayName("残高不足時にInsufficientFundsExceptionがスローされる")
void processPayment_withInsufficientFunds_shouldThrowException() {
    // Given
    request.setCustomerId("INSUFFICIENT_FUNDS_TEST");
    
    // When & Then
    assertThatThrownBy(() -> paymentService.processPayment(request, null))
        .isInstanceOf(InsufficientFundsException.class)
        .hasMessageContaining("残高");
}
```

#### 統合テスト (MockMvc)

```java
@Test
@DisplayName("正常: 有効なリクエストで決済成功")
void processPayment_withValidRequest_shouldReturn201() throws Exception {
    mockMvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"));
}
```

### テスト実行

```bash
# 全テスト実行
./mvnw test

# カバレッジレポート生成
./mvnw jacoco:report
```

---

## セットアップ手順

### 必要条件

- Java 17+
- Maven 3.8+
- Node.js 18+
- Docker (オプション)

### ローカル開発

```bash
# リポジトリクローン
git clone https://github.com/your-username/payment-gateway-demo.git
cd payment-gateway-demo

# バックエンド起動
cd backend/payment-service
./mvnw spring-boot:run

# フロントエンド起動 (別ターミナル)
cd frontend
npm install
npm run dev
```

### Docker Compose

```bash
# 全サービス起動
docker-compose up -d

# アクセス
# - フロントエンド: http://localhost:3000
# - バックエンドAPI: http://localhost:8080
# - Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## デプロイメント

### Docker イメージビルド

```bash
# バックエンド
docker build -t payment-service:latest \
  -f infrastructure/docker/Dockerfile.backend .

# フロントエンド
docker build -t payment-frontend:latest \
  -f infrastructure/docker/Dockerfile.frontend .
```

### Kubernetes (想定)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: payment-service
  template:
    spec:
      containers:
        - name: payment-service
          image: payment-service:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
          livenessProbe:
            httpGet:
              path: /api/v1/payments/health
              port: 8080
```

---

## 技術的選定理由

### Java 17 + Spring Boot 3.2

| 選定理由 |
|---------|
| エンタープライズ金融システムでの豊富な実績 |
| 長期サポート (LTS) による安定性 |
| Spring Securityによる堅牢なセキュリティ基盤 |
| マイクロサービス対応 (Spring Cloud) |

### MapStruct

| 選定理由 |
|---------|
| コンパイル時マッピングコード生成でランタイムオーバーヘッドなし |
| 型安全なEntity/DTO変換 |
| Lombokとの互換性 |

### PostgreSQL

| 選定理由 |
|---------|
| ACID準拠のトランザクション保証 |
| 金融データに必要な高精度数値型 |
| AWS RDS / Azure Database for PostgreSQL 対応 |

### React + TypeScript

| 選定理由 |
|---------|
| 型安全なフロントエンド開発 |
| 大規模チーム開発に適したコンポーネント設計 |
| 豊富なエコシステム |

---

## AI-Native Development

本プロジェクトは **Cursor IDE + Claude** を活用したAI駆動開発で構築されました。

### 開発効率

| 項目 | 従来手法 | AI活用 |
|------|---------|--------|
| 初期構築 | 2-3日 | 数時間 |
| テストコード作成 | 1日 | 1時間 |
| ドキュメント作成 | 半日 | 30分 |

### AI活用のポイント

1. **コード生成**: ボイラープレートコードの自動生成
2. **テスト作成**: 異常系シナリオの網羅的なテストケース生成
3. **ドキュメント**: 技術文書・コメントの自動生成
4. **リファクタリング**: コード品質の継続的改善

---

## ライセンス

MIT License

---

## 作者

Portfolio Project - Financial Payment Gateway Demo

**技術スタック**: Java / Spring Boot / React / TypeScript / Docker
