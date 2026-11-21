# AI 服务连接问题排查（2026-11-19）

## 现象与复现入口

管理员进入“大模型接入”，填写接口地址、模型名称及密钥，点击“测试连接”。用户截图显示接口为 `https://api.deepseek.com/chat/completions`，模型名称为 `DeekSeek Chat`，页面提示“模型服务暂时不可用，请稍后重试或使用酒店搜索”。

## 已确认

| Bug                                                    | 原因                                                                     | 解决方式                                                                                                                    |
| ------------------------------------------------------ | ------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------- |
| 模型名称配置不正确（待用户修正）                       | 截图使用展示名称且有拼写错误；后端将输入原样传给 model 字段              | 填写服务商支持的模型 ID。当前 DeepSeek 官方文档列出 deepseek-flash、deepseek-v4-pro，应按实际账号可用模型选择，再测试并保存 |
| 提示无法区分参数、余额和服务异常（已定位，未修改代码） | AiModelClient 仅区分 401/403 和 429；400、402、404、422 等均落入通用提示 | 后续应按上游 HTTP 状态返回脱敏的具体操作提示，避免透传可能含敏感信息的上游原文                                              |

## 代码与运行证据

- `hotel-back/src/main/java/com/school/hotel/service/AiModelClient.java`：model 取自 settings.model()；默认 HTTP 非成功分支生成截图中的通用错误。
- `hotel-back/src/main/java/com/school/hotel/controller/AdminAiController.java`：测试接口使用 candidate 配置，未调用 save；测试成功不等于配置已保存。
- `hotel-out/src/components/business/AiSettingsPanel.vue`：测试与保存为两个独立操作。
- 当前本地后端进程默认配置路径 `D:/overword/hotel-back/data/ai-settings.json` 未发现文件。截图所在环境是否与该本地进程一致未确认。

## 官方参考

- [模型与完整调用地址](https://api-docs.deepseek.com/en/)
- [模型 ID 查询接口](https://api-docs.deepseek.com/api/list-models/)
- [错误码说明](https://api-docs.deepseek.com/quick_start/error_codes/)：401 密钥认证失败、402 余额不足、400/422 请求格式或参数问题、429 限流。

## 验证边界

本次完成代码检查、默认配置文件存在性检查和官方文档核对；没有读取或输出 API 密钥，没有向模型服务发送推理请求，没有修改配置或源代码。未获得此次失败的上游具体 HTTP 状态码，因此不能断言只有模型名称问题，也不能确认密钥、余额或模型输出兼容性正常。未将故障标为已修复。

建议操作：修正模型 ID → 点击测试连接 → 成功后保存配置 → 刷新首页检查 AI 状态。若仍失败，需要继续读取脱敏状态码定位，勿将密钥发到聊天或报告中。
