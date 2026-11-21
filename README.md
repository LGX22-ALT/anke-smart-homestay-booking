# 安可智能民宿预定

安可智能民宿预定是一个前后端分离的民宿预订平台，包含用户端、商家端、管理端、订单管理、地图检索和可配置的 AI 能力。

## 代码结构

| 目录 | 说明 | 技术栈 |
| --- | --- | --- |
| [`hotel-out`](./hotel-out) | 前端应用 | Vue 3、Vite、Pinia、Vue Router、Element Plus |
| [`hotel-back`](./hotel-back) | 后端服务 | Java 17、Spring Boot 3、MyBatis-Plus、MySQL、Redis |
| [`.github/workflows`](./.github/workflows) | CI 检查 | Node.js 22、Java 17 |

## 快速开始

### 后端

1. 准备 MySQL 8，并执行 [`hotel-back/sql/hotel_booking.sql`](./hotel-back/sql/hotel_booking.sql) 及需要的迁移脚本。
2. 按 [`hotel-back/.env.example`](./hotel-back/.env.example) 配置数据库、Redis 和 JWT 环境变量。
3. 启动服务：

   ```powershell
   cd hotel-back
   .\mvnw.cmd spring-boot:run
   ```

### 前端

1. 复制 [`hotel-out/.env.example`](./hotel-out/.env.example) 为本地环境配置并填写后端地址。
2. 安装依赖并启动：

   ```bash
   cd hotel-out
   npm ci
   npm run dev
   ```

## 验证命令

```bash
cd hotel-out
npm test
npm run lint
npm run build
```

```powershell
cd hotel-back
.\mvnw.cmd test
```

本仓库不提交依赖、构建产物、运行时上传文件或本地密钥；这些内容已在根目录 `.gitignore` 中排除。
