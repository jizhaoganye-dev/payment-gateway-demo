# 金融決済ゲートウェイ・デモシステム

**AI駆動開発（AI-Driven Development）による金融グレード決済システムの高速構築**

[![Live Demo](https://img.shields.io/badge/Live%20Demo-Vercel-black?style=for-the-badge&logo=vercel)](https://payment-dashboard-demo-wine.vercel.app)
[![GitHub](https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github)](https://github.com/jizhaoganye-dev/payment-gateway-demo)

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?style=flat-square)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square)
![TypeScript](https://img.shields.io/badge/TypeScript-5-blue?style=flat-square)
![AI Powered](https://img.shields.io/badge/AI%20Powered-Cursor%20%2B%20Claude-purple?style=flat-square)

---

## 🎯 プロジェクト概要

本プロジェクトは、**大手金融機関の決済基盤モダン化**を想定したデモシステムです。

**Cursor IDE + Claude（AI）との対話型開発**により、要件定義からアーキテクチャ設計、実装、テスト、デプロイまでを**従来の数倍の速度**で完結させました。

### ライブデモ

| サービス | URL |
|---------|-----|
| **ダッシュボード** | https://payment-dashboard-demo-wine.vercel.app |
| **GitHub** | https://github.com/jizhaoganye-dev/payment-gateway-demo |

---

## 🚀 AI駆動開発（AI-Driven Development）

### 開発プロセス全体像

本プロジェクトでは、**単なるコード生成ではなく**、開発ライフサイクル全体をAIと協働して進めました。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    AI駆動開発プロセス（AI-Driven Development）            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │  1. 要件定義  │───▶│ 2. アーキテクチャ│───▶│  3. 実装     │              │
│  │   with AI    │    │    設計 with AI │    │   with AI   │              │
│  └──────────────┘    └──────────────┘    └──────────────┘              │
│         │                   │                   │                       │
│         ▼                   ▼                   ▼                       │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │ 金融要件の   │    │ マイクロサービス│    │ MapStruct    │              │
│  │ 構造化       │    │ 分離戦略の検討 │    │ 冪等性キー   │              │
│  └──────────────┘    └──────────────┘    └──────────────┘              │
│                                                                         │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │ 4. テスト    │───▶│ 5. ドキュメント│───▶│ 6. デプロイ  │              │
│  │   with AI    │    │    with AI    │    │   with AI   │              │
│  └──────────────┘    └──────────────┘    └──────────────┘              │
│         │                   │                   │                       │
│         ▼                   ▼                   ▼                       │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │ 異常系含む   │    │ OpenAPI      │    │ Docker +    │              │
│  │ 80%カバレッジ │    │ 技術文書     │    │ Vercel      │              │
│  └──────────────┘    └──────────────┘    └──────────────┘              │
│                                                                         │
│  【使用ツール】 Cursor IDE + Claude (Anthropic)                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### フェーズ別AI活用詳細

#### Phase 1: 要件定義 × AI

```
【対話例】
Human: 「107万円の金融決済モダン化案件に採用されるポートフォリオを作りたい」

AI: 以下の要件を構造化しました：
    - マイクロサービス化を前提としたAPI設計
    - 金融グレードのエラーハンドリング（冪等性保証）
    - JUnitによる高カバレッジテスト
    - OpenAPIによるプロフェッショナルなドキュメント
```

**成果物**: 金融システム特有の要件（ACID特性、二重処理防止、監査証跡）を網羅した設計方針

#### Phase 2: アーキテクチャ設計 × AI

```
【対話例】
Human: 「モノリスからマイクロサービスへの移行を想定した設計にしたい」

AI: 以下のアーキテクチャを提案します：
    - レイヤードアーキテクチャ（Controller → Service → Repository）
    - ドメイン境界の明確化（Payment, Notification, Reporting, Fraud Detection）
    - MapStructによるEntity/DTO分離
    - 冪等性キーエンティティの導入
```

**成果物**: マイクロサービス移行を見据えた疎結合設計

#### Phase 3: 実装 × AI

AIとの対話により、以下のコードを高速生成：

| コンポーネント | 生成内容 | 特徴 |
|--------------|---------|------|
| `TransactionMapper` | Entity⇔DTOマッピング | MapStructによるコンパイル時生成 |
| `IdempotencyService` | 二重処理防止 | SHA-256ハッシュ、24時間TTL |
| `GlobalExceptionHandler` | 統一エラーハンドリング | 15種類以上のエラーパターン |
| `PaymentController` | REST API | OpenAPI 3.0 完全対応 |

#### Phase 4: テスト × AI

```java
// AIが生成した異常系テストの例
@Test
@DisplayName("残高不足時にInsufficientFundsExceptionがスローされる")
void processPayment_withInsufficientFunds_shouldThrowException() {
    request.setCustomerId("INSUFFICIENT_FUNDS_TEST");
    
    assertThatThrownBy(() -> paymentService.processPayment(request, null))
        .isInstanceOf(InsufficientFundsException.class)
        .hasMessageContaining("残高");
}
```

**テストカバレッジ**: 80%以上（異常系シナリオ含む）

---

## 🔄 モダン化シミュレーション（Modernization）

### Before → After: モノリスからの脱却

本プロジェクトでは、**レガシーなモノリス環境を想定**し、AIを活用して**関心の分離（Separation of Concerns）**を実現しました。

#### Before: モノリス時代の課題

```java
// ❌ 問題のあるレガシーコード例
public class PaymentServlet extends HttpServlet {
    // 1. Controller/Service/Repositoryが混在
    // 2. トランザクションIDがString型で型安全性なし
    // 3. 二重処理防止なし
    // 4. エラーハンドリングが散在
    
    protected void doPost(HttpServletRequest req, HttpServletResponse res) {
        String amount = req.getParameter("amount");  // バリデーションなし
        String sql = "INSERT INTO transactions VALUES ('" + amount + "')";  // SQLインジェクション脆弱性
        // ... 500行以上のメソッド
    }
}
```

#### After: クリーンアーキテクチャ

```java
// ✅ AIと共にリファクタリングした結果
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    
    private final PaymentService paymentService;  // 依存性注入
    
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request,  // jakarta.validation
            @RequestHeader("X-Idempotency-Key") String idempotencyKey  // 冪等性保証
    ) {
        // Controllerはリクエスト受付のみ
        PaymentResponse response = paymentService.processPayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
```

### リファクタリング戦略

| 改善項目 | Before | After |
|---------|--------|-------|
| **アーキテクチャ** | Servlet直書き | レイヤードアーキテクチャ |
| **型安全性** | String型多用 | Enum + DTO |
| **バリデーション** | 手動チェック | jakarta.validation |
| **エラー処理** | try-catch散在 | @RestControllerAdvice |
| **二重処理防止** | なし | 冪等性キー |
| **監査証跡** | 手動ログ | JPA Auditing |
| **テスト** | 手動テスト | JUnit 5 + Mockito |

---

## 🛡️ 品質担保

### AIによる静的解析とテストの同時並行作成

```
┌─────────────────────────────────────────────────────────────┐
│                    品質保証プロセス                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐   │
│  │ 実装コード  │────▶│ AIレビュー  │────▶│ テスト生成  │   │
│  │   生成     │     │ & リファクタ │     │   80%+     │   │
│  └─────────────┘     └─────────────┘     └─────────────┘   │
│         │                  │                  │            │
│         ▼                  ▼                  ▼            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │            同時並行での品質向上                       │   │
│  │  - Lombok/MapStruct による定型コード削減              │   │
│  │  - 例外クラスの細分化（金融グレード）                 │   │
│  │  - 異常系シナリオの網羅的テスト                       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 実装済み品質機能

| カテゴリ | 実装内容 |
|---------|---------|
| **金融グレード精度** | `BigDecimal` (precision=19, scale=4) |
| **冪等性保証** | `X-Idempotency-Key` + SHA-256ハッシュ |
| **トランザクション** | `@Transactional(isolation=READ_COMMITTED)` |
| **入力検証** | jakarta.validation（金額マイナス禁止等） |
| **エラー統一** | RFC 7807準拠のJSON形式 |
| **テスト** | JUnit 5 + Mockito + MockMvc |

### テストカバレッジ

```
┌────────────────────────────────────────────────────────────┐
│                     テストピラミッド                        │
├────────────────────────────────────────────────────────────┤
│                                                            │
│              ╱╲                                            │
│             ╱  ╲         E2E Tests                         │
│            ╱────╲        (ブラウザ自動化)                   │
│           ╱      ╲                                         │
│          ╱────────╲      Integration Tests                 │
│         ╱          ╲     (MockMvc による API テスト)        │
│        ╱────────────╲                                      │
│       ╱              ╲   Unit Tests                        │
│      ╱────────────────╲  (Service層 + 異常系シナリオ)       │
│                                                            │
│  【カバレッジ目標: 80%以上】                                │
│  - 正常系: 全決済方法、全通貨対応                          │
│  - 異常系: 残高不足、無効カード、タイムアウト              │
│  - 境界値: 最小金額(1円)、最大金額                         │
│  - 冪等性: キャッシュ返却、衝突検出                        │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## ⚡ 生産性の数値化

### AI活用による開発効率

| 工程 | 従来手法（想定） | AI活用（実績） | 効率化率 |
|------|-----------------|---------------|---------|
| 要件定義・設計 | 2日 | 2時間 | **12倍** |
| バックエンド実装 | 3日 | 4時間 | **6倍** |
| フロントエンド実装 | 2日 | 3時間 | **5倍** |
| テストコード作成 | 2日 | 2時間 | **12倍** |
| ドキュメント作成 | 1日 | 1時間 | **8倍** |
| デプロイ設定 | 0.5日 | 30分 | **8倍** |
| **合計** | **10.5日** | **約12時間** | **約7倍** |

### 具体的な効率化ポイント

1. **ボイラープレート削減**
   - Lombok + MapStruct で定型コードを80%削減
   - AIによる適切なアノテーション提案

2. **異常系の網羅**
   - AIが金融システム特有のエラーパターンを提案
   - 残高不足、無効カード、タイムアウト等を漏れなくカバー

3. **ドキュメント自動化**
   - OpenAPIアノテーションの自動生成
   - 日本語技術文書の高品質作成

4. **リファクタリング支援**
   - コード品質の継続的改善
   - デザインパターンの適切な適用

---

## 🏗️ 技術スタック

### バックエンド

| 技術 | バージョン | 選定理由 |
|------|-----------|---------|
| Java | 17 LTS | エンタープライズ金融システムでの実績 |
| Spring Boot | 3.2 | マイクロサービス対応、長期サポート |
| Spring Data JPA | 3.2 | 型安全なデータアクセス |
| MapStruct | 1.5.5 | コンパイル時マッピング、ランタイムオーバーヘッドなし |
| H2 / PostgreSQL | - | 開発/本番環境の柔軟な切り替え |
| SpringDoc OpenAPI | 2.3 | Swagger UI自動生成 |

### フロントエンド

| 技術 | バージョン | 選定理由 |
|------|-----------|---------|
| React | 18 | コンポーネント設計、大規模開発対応 |
| TypeScript | 5 | 型安全性、リファクタリング容易性 |
| Vite | 5 | 高速ビルド、HMR |
| TailwindCSS | 3 | ユーティリティファースト、保守性 |
| TanStack Query | 5 | サーバー状態管理 |

### インフラ

| 技術 | 用途 |
|------|------|
| Docker | マルチステージビルド、非rootユーザー実行 |
| Nginx | 静的配信、リバースプロキシ |
| Vercel | フロントエンドホスティング |
| GitHub | バージョン管理、CI/CD連携 |

---

## 📁 プロジェクト構成

```
payment-gateway-demo/
├── backend/
│   └── payment-service/
│       └── src/main/java/com/fintech/payment/
│           ├── controller/       # REST API（リクエスト受付のみ）
│           ├── service/          # ビジネスロジック集約
│           ├── repository/       # データアクセス
│           ├── entity/           # JPAエンティティ
│           ├── dto/              # データ転送オブジェクト
│           ├── mapper/           # MapStructマッパー
│           ├── exception/        # 統一例外ハンドリング
│           └── config/           # 設定クラス
├── frontend/
│   └── src/
│       ├── components/           # Reactコンポーネント
│       ├── pages/                # ページコンポーネント
│       ├── services/             # API通信
│       └── types/                # TypeScript型定義
├── infrastructure/
│   └── docker/
│       ├── Dockerfile.backend    # マルチステージビルド
│       ├── Dockerfile.frontend   # Nginx配信
│       └── nginx.conf            # リバースプロキシ設定
└── docker-compose.yml            # ローカル開発環境
```

---

## 🔧 セットアップ

### ローカル開発

```bash
# リポジトリクローン
git clone https://github.com/jizhaoganye-dev/payment-gateway-demo.git
cd payment-gateway-demo

# バックエンド起動
cd backend/payment-service
./mvnw spring-boot:run

# フロントエンド起動（別ターミナル）
cd frontend
npm install
npm run dev
```

### Docker Compose

```bash
docker-compose up -d

# アクセス
# - ダッシュボード: http://localhost:3000
# - API: http://localhost:8080
# - Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## 📚 API仕様

| メソッド | エンドポイント | 説明 |
|---------|---------------|------|
| POST | `/api/v1/payments` | 決済処理（冪等性キー対応） |
| GET | `/api/v1/payments/{id}` | トランザクション取得 |
| POST | `/api/v1/payments/{id}/refund` | 返金処理 |
| GET | `/api/v1/payments/merchant/{id}/summary` | 売上サマリー |

**Swagger UI**: ローカル起動後 `http://localhost:8080/swagger-ui.html`

---

## 🎓 本プロジェクトが証明するスキル

| スキル領域 | 具体的な実装 |
|-----------|-------------|
| **Java/Spring Boot** | REST API、DI、AOP、トランザクション管理 |
| **マイクロサービス設計** | レイヤードアーキテクチャ、ドメイン分離 |
| **金融システム知識** | 冪等性、ACID、BigDecimal精度 |
| **フロントエンド** | React、TypeScript、状態管理 |
| **テスト駆動** | JUnit 5、Mockito、異常系カバレッジ |
| **AI活用** | Cursor + Claude による高速開発 |
| **DevOps** | Docker、CI/CD、クラウドデプロイ |

---

## 📞 お問い合わせ

本プロジェクトに関するご質問は、GitHubのIssueまたはPull Requestでお受けしています。

---

**Built with AI-Driven Development using Cursor + Claude**

*このプロジェクトは、AI技術を活用した次世代の開発手法を実証するものです。*
