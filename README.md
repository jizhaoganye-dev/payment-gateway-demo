# 🏦 Payment Gateway Demo

**金融グレード決済ゲートウェイ - フルスタック・マイクロサービスアプリケーション**

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue?logo=typescript)](https://www.typescriptlang.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](https://www.docker.com/)

---

## 🎯 概要

本プロジェクトは、**大手金融機関向け決済基盤モダン化**を想定したデモアプリケーションです。

### 主な特徴

- 🏗️ **マイクロサービスアーキテクチャ** - モノリスからの移行を実証
- 🔐 **金融グレードの設計** - 厳格なバリデーション、監査ログ
- 📊 **リアルタイムダッシュボード** - React + RechartsによるUI
- 📝 **OpenAPI ドキュメント** - Swagger UIによる自動生成
- 🐳 **コンテナ対応** - Docker Compose による一括起動
- 🤖 **AI活用開発** - Cursor/Claudeによる高速実装

---

## 🖼️ スクリーンショット

### ダッシュボード
![Dashboard](docs/screenshots/dashboard.png)

### 取引履歴
![Transactions](docs/screenshots/transactions.png)

### API ドキュメント
![Swagger](docs/screenshots/swagger.png)

---

## 🛠️ 技術スタック

### Backend
| 技術 | バージョン | 用途 |
|------|-----------|------|
| Java | 17 LTS | メイン言語 |
| Spring Boot | 3.2 | フレームワーク |
| Spring Data JPA | 3.2 | データアクセス |
| PostgreSQL / H2 | 15 / 2 | データベース |
| OpenAPI | 3.0 | APIドキュメント |
| JUnit 5 | 5.10 | テスト |

### Frontend
| 技術 | バージョン | 用途 |
|------|-----------|------|
| React | 18 | UIライブラリ |
| TypeScript | 5 | 型安全 |
| Vite | 5 | ビルドツール |
| TailwindCSS | 3.4 | スタイリング |
| Recharts | 2.10 | データ可視化 |
| React Query | 5 | データフェッチ |

### Infrastructure
| 技術 | 用途 |
|------|------|
| Docker | コンテナ化 |
| Docker Compose | オーケストレーション |
| Nginx | リバースプロキシ |

---

## 📁 プロジェクト構造

```
payment-gateway-demo/
├── backend/
│   └── payment-service/
│       ├── src/main/java/com/fintech/payment/
│       │   ├── controller/     # REST API
│       │   ├── service/        # ビジネスロジック
│       │   ├── repository/     # データアクセス
│       │   ├── entity/         # ドメインモデル
│       │   ├── dto/            # データ転送オブジェクト
│       │   ├── exception/      # 例外ハンドリング
│       │   ├── config/         # 設定クラス
│       │   └── logging/        # AOP ロギング
│       ├── src/test/           # テストコード
│       └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/         # 共通コンポーネント
│   │   ├── pages/              # ページコンポーネント
│   │   ├── services/           # API クライアント
│   │   ├── types/              # 型定義
│   │   └── hooks/              # カスタムフック
│   └── package.json
│
├── infrastructure/
│   └── docker/
│       ├── Dockerfile.backend
│       ├── Dockerfile.frontend
│       └── nginx.conf
│
├── docs/
│   ├── architecture/
│   └── refactoring/
│       └── REFACTORING_STRATEGY.md  # リファクタリング戦略
│
└── docker-compose.yml
```

---

## 🚀 クイックスタート

### 前提条件

- Java 17+
- Node.js 20+
- Docker & Docker Compose (オプション)

### ローカル起動

```bash
# 1. リポジトリをクローン
git clone https://github.com/jizhaoganye-dev/payment-gateway-demo.git
cd payment-gateway-demo

# 2. バックエンド起動
cd backend/payment-service
./mvnw spring-boot:run

# 3. フロントエンド起動 (別ターミナル)
cd frontend
npm install
npm run dev
```

### Docker で起動

```bash
docker-compose up --build
```

### アクセス先

| サービス | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |

---

## 📡 API エンドポイント

| Method | Endpoint | 説明 |
|--------|----------|------|
| POST | `/api/v1/payments` | 決済処理実行 |
| GET | `/api/v1/payments/{id}` | トランザクション取得 |
| GET | `/api/v1/payments/merchant/{id}` | 加盟店トランザクション一覧 |
| POST | `/api/v1/payments/{id}/refund` | 返金処理 |
| GET | `/api/v1/payments/merchant/{id}/summary` | 売上サマリー |
| GET | `/api/v1/payments/health` | ヘルスチェック |

詳細は [Swagger UI](http://localhost:8080/swagger-ui.html) を参照してください。

---

## 🧪 テスト

```bash
# ユニットテスト実行
cd backend/payment-service
./mvnw test

# カバレッジレポート生成
./mvnw jacoco:report
```

---

## 📊 リファクタリング戦略

モノリスからマイクロサービスへの移行戦略は [REFACTORING_STRATEGY.md](docs/refactoring/REFACTORING_STRATEGY.md) を参照してください。

### 主な改善点

- ✅ 巨大メソッド（500行）→ 小さなメソッド（30行以内）
- ✅ String型ステータス → Enum型で型安全
- ✅ 手動ログ → AOPによる自動ログ
- ✅ 個別例外処理 → グローバル例外ハンドラー
- ✅ モノリスDB → サービス別DB（将来対応）

---

## 🤖 AI活用開発

本プロジェクトは **AI Native 開発手法** で構築されました。

- **Cursor IDE** - AI支援コーディング
- **Claude** - 設計レビュー、ドキュメント生成
- **GitHub Copilot** - コード補完

### 開発効率

| 項目 | 従来手法 | AI活用 | 効率化 |
|------|---------|--------|--------|
| 初期実装 | 5日 | 1日 | 5x |
| テストコード | 2日 | 0.5日 | 4x |
| ドキュメント | 1日 | 2時間 | 4x |

---

## 📄 ライセンス

MIT License

---

## 👤 開発者

**AI Native Engineer**

- AI協働開発のスペシャリスト
- Java/Spring Boot、React/TypeScriptに精通
- 金融システム開発経験

---

⭐ このプロジェクトが参考になったら、Starをお願いします！
