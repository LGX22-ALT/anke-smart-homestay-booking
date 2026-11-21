# 线上 AI 版本同步更新

2026-11-19：用户确认 `http://blog.liangguoxi.icu/` 管理后台保存 AI 配置报错。

已核对线上静态资源与后端 OpenAPI：前端调用 `/api/admin/ai/connections`，线上旧后端只有 `/settings`、`/test`，缺少连接管理接口。当前公开 AI 状态为 `enabled:false`。需要更新正在运行的后端。

## 配套产物

- 文件：本项目 `hotel-back/target/hotel-back-0.1.0.jar`，本轮执行 `mvnw.cmd package` 生成。
- 大小：56,468,957 字节。
- SHA-256：`aa6e3d77ab6b3a87bf4972a31973ce38b98a261182100e20094325b4f35f622a`。
- 已检查 JAR 内 `AdminAiController.class` 包含连接管理、激活及测试接口。
- 构建成功，100 项测试通过，2 项按环境条件跳过，0 失败、0 错误。跳过的是完整预订浏览器流程和资料浏览器流程，本轮未重跑这两项。

## 宝塔更新与验收

1. 在宝塔 Java 项目管理中找到当前监听 `8081`、供该域名使用的后端，核对实际 JAR 路径、工作目录与启动配置。备份当前 JAR 与原有 AI 配置文件；配置可能含密钥，留在服务器私有目录。
2. 上传本轮 JAR，替换该项目的运行产物。保留现有数据库、Redis、JWT、上传目录等生产配置。不要导入或覆盖数据库，本次接口升级不需要数据库迁移。
3. 重启这个 Java 项目，确认启动成功且仍监听 `8081`。只上传文件不会更新正在运行的 Java 进程。以实际启动配置中的 JAR 路径为准。
4. 登录 `/admin/ai`，添加或编辑模型，测试连接、勾选「启用此连接」并保存。已有其他当前连接时，切换到这次配置的连接。测试成功本身不会保存或激活表单。
5. 在浏览器 Network 中确认 `GET /api/admin/ai/connections` 与保存时的 POST/PUT 返回业务 `code:200`。访问 `/api/ai/status`，确认 `data.enabled:true`，最后在首页验证一次 AI 搜索。

如果保存后重启又恢复禁用，应核对 Java 工作目录、`HOTEL_AI_CONFIG_PATH` 指向的持久文件和运行用户读写权限。默认配置是工作目录内的 `data/ai-settings.json`，不随前端发布或数据库导入迁移。

## 验证边界

本轮没有服务器管理会话，尚未替换线上 JAR、保存线上配置或调用真实付费模型，不能宣称线上修复完成。无需发送服务器密码、API Key 或 Cookie。更新后若仍报错，核对失败接口的状态、业务错误文字及对应服务器日志，输出前去除敏感内容。

## 后续产物更新

同日商家私信与头像展示任务已重新生成同路径 JAR。上方 SHA-256 仅对应当次 AI 排查产物；本轮最新版本与校验值请使用 [私信交付记录](../hotel-out/docs/product/2026-11-19-merchant-chat.md) 及 [产物清单](../hotel-out/test-artifacts/merchant-chat-artifact.json)。
