# AI 住宿助手接入

## 使用

1. 重启后端 Spring Boot 服务（当前默认端口 `8081`），刷新前端。
2. 管理员进入侧栏「大模型接入」（`/admin/ai`）；原「系统配置 → AI 助手」入口也可使用，两处共用配置。
3. 点击「添加模型」，填写连接名称、接口地址、模型 ID 和 API Key。
4. 点击「测试连接」，成功后勾选「启用此连接」并保存。在连接列表确认它是当前生效模型；已有其他激活连接时，需点击切换模型。
5. 回首页刷新，即可输入目的地、日期、人数和每晚预算；也可以继续补充条件。

接口必须支持 `messages`、`response_format: {"type":"json_object"}` 以及
`choices[0].message.content` JSON 响应。填写服务商实际提供的模型 ID，项目不固定模型版本。
例如使用 DeepSeek 时，完整地址为 `https://api.deepseek.com/chat/completions`，
具体模型名称和 JSON 模式要求以 [官方文档](https://api-docs.deepseek.com/guides/json_mode/) 为准。
本地模型可使用 `http://127.0.0.1:端口/v1/chat/completions`；远程地址要求 HTTPS。

## 数据与预订流程

- 前端只向本系统发送用户问题和最多 6 条历史消息；搜索不会向模型发送手机号、证件或订单信息。
- 模型只提取城市、日期、人数、预算、关键词、设施和排序条件。
- 后端重新校验日期、价格、人数与排序白名单，调用既有 `HotelService` 查询真实已上架房源。
- 带日期时使用现有房态查询；不指定日期时仅展示住宿，提醒补充日期。
- 返回最多 3 家房源以及「查看全部结果」。选择房源进入原详情页选房型；创建订单仍须登录、填写入住信息并确认。
- 不会由模型生成房源数据、执行 SQL、创建订单或付款。非住宿问题会提示支持范围。
- 未配置、关闭、接口出错或 Mock 模式均保留普通酒店搜索入口，不使用假 AI 回答。

## 配置保存

本步骤无须改表或重新导入数据库。配置默认保存为后端工作目录下的 `data/ai-settings.json`，
可用环境变量 `HOTEL_AI_CONFIG_PATH` 指定绝对路径。重启会保留配置；多实例部署需共享配置或另行接入统一配置中心。

这是包含明文 API Key 的服务器配置文件，仅供后端进程读取，不在静态资源目录中。
`data/` 已加入 `.gitignore`，请为服务器账号设置适当文件权限，并不要将它放入上传目录、前端或版本库。
后台接口仅管理员可访问；读取配置只返回 `hasApiKey`，不回传密钥。密钥栏留空保持原值，勾选清除可以移除。
连接测试使用当前表单但不保存配置；只有点击保存才生效。

## 接口

新版连接管理接口如下，均仅管理员可访问，读取时不返回密钥。

| 接口 | 用途 |
| --- | --- |
| `GET /api/admin/ai/connections` | 列出模型连接 |
| `POST /api/admin/ai/connections` | 新建连接 |
| `PUT /api/admin/ai/connections/{id}` | 保存已有连接 |
| `PUT /api/admin/ai/connections/{id}/activate` | 切换当前生效连接 |
| `DELETE /api/admin/ai/connections/{id}` | 删除非当前连接 |
| `POST /api/admin/ai/connections/test` | 测试表单，尚不保存 |
| `POST /api/admin/ai/connections/{id}/test` | 测试已保存连接 |

公开接口与旧版兼容接口：

| 接口 | 权限 | 用途 |
| --- | --- | --- |
| `GET /api/ai/status` | 公开 | 仅返回启用状态 |
| `POST /api/ai/search` | 公开 | 浏览型 AI 找房，单实例每来源地址每分钟最多 10 次、最多 4 个并发 |
| `GET /api/admin/ai/settings` | 管理员 | 获取脱敏配置 |
| `PUT /api/admin/ai/settings` | 管理员 | 保存模型配置 |
| `POST /api/admin/ai/test` | 管理员 | 测试当前表单连接 |

开发代理会将来源显示为本机地址，限流将共享该地址；生产多实例或反向代理部署需要在网关按可信真实来源统一限流。
模型连接最长等待约 30 秒，前端为 AI 调用单独配置 40 秒等待，不改变其他接口超时。

## 验证

前后端需要配套更新。新版页面调用 `/api/admin/ai/connections`，仅更新前端而继续运行旧版后端，会导致列表、保存和连接测试失败。线上更新方法见 [生产更新说明](AI_PRODUCTION_UPDATE.md)。

后端 `mvnw.cmd test` 包含配置持久化、密钥不回显、权限、参数校验、真实房源返回与模拟模型 HTTP 协议测试。
浏览器测试脚本位于 `hotel-out/scripts/ai_assistant_test.py`，使用拦截的测试响应检查 UI，不会调用付费模型。
实际服务商连接需要管理员提供有效密钥后测试。
