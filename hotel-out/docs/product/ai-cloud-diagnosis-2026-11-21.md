# 线上 AI 模型连接列表加载错误排查

日期：2026-11-21。状态：已确认前后端接口版本不一致；待云端更新后端，未修复验收。

## 问题与影响

用户在 `/admin/ai` 看到模型连接列表为空，并显示“服务器内部错误”。当前前端使用模型连接库，需要 `/api/admin/ai/connections`；列表读取失败时 items 保持初始空数组，所以页面的“0 个连接”“尚未接入模型”不能作为配置确实为空或已丢失的证据。

## 实际证据和根因

1. 静态检查 `src/api/modules/ai.js:18` 与 `AiConnectionLibrary.vue` 的 load 方法：页面加载调用连接列表接口；该操作不调用外部模型。
2. 只读请求线上 `http://103.212.186.243:8081/v3/api-docs`，HTTP 200；AI 接口仅包括 `/api/admin/ai/settings`、`/api/admin/ai/test`、`/api/ai/search`、`/api/ai/status`，缺少连接列表及创建、切换等新版接口。
3. 当前源码 `AdminAiController.java:24` 已提供 `/connections` 等新接口。本地 `target/hotel-back-0.1.0.jar` 内控制器字节码也包含 `/connections`、`/connections/{id}/activate`，与线上接口集合不同，证明线上运行包尚未包含新版连接库接口。
4. 域名代理及直接 8081 的公开 AI 状态接口均 HTTP/业务 200，enabled=false。公开状态 enabled=false 不能证明管理列表接口正常，也不是密钥校验结果。

可确认的故障是前端版本与线上后端接口不匹配。现有通用异常处理会将未单独处理的异常归为业务 500；缺失接口被该处理捕获是合理解释，但尚未取得用户登录后的错误响应和服务器堆栈，不能把具体异常类型写成已复现。没有使用管理员凭证或调用收费模型来验证。

## 解决方式与操作位置

- 在宝塔网站的 Java 项目中备份旧运行 JAR，停止原进程，上传当前 `hotel-back/target/hotel-back-0.1.0.jar` 并确认项目指向该文件，再启动。仅替换磁盘文件但不重启，运行中的应用不会更新。
- 保持 Java 17、8081、prod、数据库/Redis 参数及原 `HOTEL_AI_CONFIG_PATH` 和工作目录。AI 连接库存储在服务端配置文件，不是 MySQL；本次接口更新无需重新导入 SQL。保留现有配置文件，不删除或公开其中凭证。
- 如果部署前源码又有修改，应在 JDK 17 环境重新执行 `mvnw.cmd clean package`，使用新构建的 JAR。当前检查只验证了现有 JAR 含接口，没有在本次重新执行构建/测试。

## 验收步骤

1. 重启后查看该运行实例的 `/v3/api-docs`，确认出现 `/api/admin/ai/connections`。
2. 管理员重新打开 AI 页面，Network 中列表请求应返回业务 code 200、data 数组。没有配置时正常空列表，而不是 500。
3. 若仍报错，取得该请求的状态码/脱敏响应及 Java 日志中 `Unhandled server exception` 附近的异常；不要提交 Authorization、Cookie、模型密钥或完整含凭证配置文件。
4. 模型保存、持久化、测试连接和真实推理是后续验收，本次没有执行。

## 改动和证据路径

本次只新增本报告及脱敏检查记录，没有修改前后端业务代码、Nginx、数据库或云端文件。

- `hotel-out/test-artifacts/ai-cloud-20261121/public-interface-check.json`：公开状态和线上 AI 路径集合。
- `hotel-out/test-artifacts/ai-cloud-20261121/local-jar-check.json`：本地 JAR 的大小、SHA-256 和新增接口存在性。
- 已检查本地 JAR：56,471,879 字节；SHA-256 `b368ba925fc294e46ce7400615f6ccf3e64d3604655ea8f14bd032ad0822e20a`。

限制：未获得服务器运行日志、管理员会话与远程 shell，无法代为更新云端或确认最终恢复。本次定位不涉及 UI 重设计，不触发视觉设计流程。
