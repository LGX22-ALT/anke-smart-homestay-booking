# 民宿酒店预订平台前端

基于 Vue 3、Vite、Pinia、Vue Router 和 Element Plus。

## 启动

```bash
npm install
npm run dev
```

## 数据源切换

- `VITE_USE_MOCK=false`：调用 `hotel-back` 的 REST API，开发环境默认使用该模式。
- `VITE_USE_MOCK=true`：使用浏览器本地 Mock，适合后端未启动时演示。

开发服务器将 `/api` 和 `/uploads` 转发至 `VITE_BACKEND_URL`，仓库开发环境配置为 `http://127.0.0.1:8081`。这个代理只在 `npm run dev` 中生效；静态生产部署由 Nginx 转发。

## 检查

```bash
npm run lint
npm run build
```

## 宝塔生产部署

适用于域名根目录部署，Java 后端直接运行在同一台服务器的 `8081` 端口。后端若在其他主机或容器中，先修改 Nginx 的 upstream 地址，不能直接沿用本机地址。

### 打包与上传

使用 Node.js 22.12+（与 CI 的 Node 22 系列一致），在 PowerShell 执行：

```powershell
cd D:\overword\hotel-out
npm ci
npm test
npm run lint
npm run build
New-Item -ItemType Directory -Force release | Out-Null
Compress-Archive -Path dist\* -DestinationPath release\hotel-frontend-baota.zip -Force
```

默认 `npm run build` 读取 `.env.production`：`VITE_USE_MOCK=false`，`VITE_API_BASE_URL=/api`。请勿使用 `--mode test`，它会启用 Mock。构建前确认系统环境变量及 `.env.production.local` 没有覆盖这两个值。任何 `VITE_` 变量都应按公开前端配置处理，不填写数据库密码或模型密钥。

宝塔中创建静态网站，把 ZIP 上传并解压到网站根目录。根目录应直接有 `index.html`、`assets/` 和 `attractions/`，不要再套一层 `dist/`。运行目录设为该根目录。只上传静态产物，不上传源码、node_modules、后端配置或数据库文件。

本次已生成的包位于 `release/hotel-frontend-baota.zip`。修改前端后需要重新构建、压缩和上传；旧包不会自动更新。

2026-11-18 公网 HTTP 预订故障修复包为 `release/hotel-frontend-fix-20261118.zip`，包含预订请求号兼容修复、正式登录页演示入口隔离和订单确认页缺图处理。上传步骤、实际验证和 HTTPS 待处理项见 [线上预订故障报告](./docs/product/live-booking-audit-2026-11-18.md)。此修复包尚未自动部署到服务器。

### 配置 Nginx

打开网站 → 设置 → 配置文件，把 [deploy/baota-locations.conf](./deploy/baota-locations.conf) 的内容合并到该网站已有的 `server { }` 内。它是配置片段，不是整份 nginx.conf，不要上传到公开网站根目录。

保留宝塔生成的域名、监听端口、网站根目录及 SSL 证书配置。替换已有的 `location /`；`client_max_body_size` 若已在同一 server 层设置则修改原值，不重复添加。清除重复的 `/api/`、`/uploads/`、`/assets/`、`/index.html` 规则，避免与宝塔“反向代理”或“伪静态”页面重复配置。移除宝塔默认针对图片、JS、CSS 的正则缓存规则，由本片段处理构建资源缓存和入口更新。

`proxy_pass http://127.0.0.1:8081;` 末尾不要添加 `/`，后端需要保留请求中的 `/api` 前缀。代理路径规则使用 `^~`，防止图片正则规则截获 `/uploads/`。总请求大小为 50MB，单张图片仍受后端 10MB 限制；API 读取超时为 60 秒，覆盖当前 AI 接口等待时间。

本项目的 `/attractions` 既是 Vue 页面地址，也是图片目录名，所以回退规则使用 `try_files $uri /index.html;`，不要加 `$uri/` 目录匹配，否则景点页面可能跳转为目录并返回 403；`/attractions/xxx.jpg` 仍正常读取静态图片。

在宝塔检查配置并重新加载 Nginx。无需在生产环境运行 Vite 开发服务器或 `npm run preview`。

### 上线验收和更新

1. 访问首页和 `/search`，直接刷新详情页或登录页，确认没有 Nginx 404。
2. 浏览器 Network 中 `/api/hotels`、`/api/ai/status` 应访问当前网站域名，响应应为后端 JSON，不应为 index.html；确认没有请求浏览者电脑的 localhost。
3. 使用测试账号验证真实登录、搜索和订单；上传图片并确认 `/uploads/` 可访问。登录或响应含凭证时不要保存 HAR/请求头等敏感证据。
4. 检查服务器的 MySQL、Redis、上传目录权限和后端日志。SQL 导入不包含本地上传图片，图片要单独迁移到后端上传目录。
5. 更新时先备份现有静态目录，再上传新文件。先传新 assets，最后替换 index.html；保留旧哈希资源一段时间，避免已打开页面加载旧模块失败。回滚时恢复匹配的入口文件及静态资源。

前端构建不代表云端业务验证通过。本次检查及证据见 [部署报告](./docs/product/baota-deployment-results.md)。

## 管理员 AI 快捷配置

在“大模型接入”选择 DeepSeek、通义千问、Kimi、智谱、硅基流动或 OpenAI，接口与默认模型自动填入。填写相应服务商 API Key，勾选启用，测试成功后保存。其他地域、网关或模型使用“自定义”。更换接口需要对应密钥，原服务商密钥不会自动用于新接口。升级后重启后端，使预设参数适配生效。详见 [功能与验收记录](docs/product/ai-presets-2026-11-19.md)。
