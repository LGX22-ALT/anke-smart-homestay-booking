# 管理员大模型快捷接入

## 需求与设计

管理员希望选择常见服务后只填写 API Key，无需逐个查找完整接口地址及模型 ID，并保留自定义服务入口。

沿用既有配置 API 和服务端文件存储：在前端新增预设目录，根据已保存的 endpoint/model 识别服务商，不新增数据库表、不修改现有凭证文件。与维护多套原生 SDK 相比，采用项目已有 Chat Completions + JSON 协议，缩小维护范围。没有匹配预设的旧配置原样归入自定义，不自动替换。

界面采用项目现有紫色品牌和原生表单，桌面三列、手机两列服务商选择按钮；固定接口只读展示，模型使用下拉选择，自定义才显示地址和 ID 输入框。无需新增动效。已按 UIUX → Frontend Design → Taste → GSAP → Impeccable 顺序核对本模块；Impeccable 引擎下载失败，使用项目 DESIGN.md 和浏览器检查，不宣称专用引擎检查通过。

实施顺序：预设及凭证隔离测试 → 前端目录和交互 → 后端参数适配与错误提示 → 构建、相关单元测试 → 浏览器主流程和窄屏检查。

## 已新增

| 服务商   | 默认模型         | 说明                                                                     |
| -------- | ---------------- | ------------------------------------------------------------------------ |
| DeepSeek | deepseek-flash   | 可切换 deepseek-v4-pro                                                   |
| 通义千问 | qwen-plus        | 百炼北京地域；其他地域或专属域名走自定义                                 |
| Kimi     | kimi-k3.1-pro    | 可切换 kimi-k3；不使用官方已下线的 K2.5 等 ID                            |
| 智谱 GLM | glm-4.7-flash    | 可切换 glm-4.7                                                           |
| 硅基流动 | Qwen/Qwen3.6-27B | 国内接口                                                                 |
| OpenAI   | gpt-4.1-mini     | 官方 Chat Completions 接口                                               |
| 自定义   | 管理员填写       | 完整接口地址、模型 ID、API Key，需兼容 Chat Completions 和 JSON 对象输出 |

每个预设提供官方控制台链接。选择预设自动填充完整接口和默认模型，管理员只需填入对应密钥，启用助手，测试连接并保存。模型权限、区域、余额、服务可用性仍取决于供应商账号。

## 确认问题与修复

| Bug                                          | 原因                                               | 解决方式                                                                                                                                  |
| -------------------------------------------- | -------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| 模型 ID 和地址依赖手填，容易输入产品展示名称 | 原配置只提供两个自由输入框                         | 加入六类预设与自定义；固定 ID 下拉选择，接口自动填充                                                                                      |
| 切换接口可能把原服务商密钥用于新接口         | 后端对空白 apiKey 无条件复用已保存密钥             | 前端切换服务商/修改地址时清空新输入；后端只有 endpoint 完全相同时才允许复用旧密钥，跨接口启用或测试必须提供新密钥                         |
| 未开启的新配置仍可能保留上一服务商密钥       | 关闭助手时不校验配置完整性                         | 更换地址并关闭助手、未提供新密钥时，保存空密钥，不沿用旧密钥                                                                              |
| 余额、模型参数或地址错误统一显示暂不可用     | 非 401/403/429 状态落入默认分支                    | 为 400/422、402、404 提供脱敏操作提示，不转发供应商错误正文                                                                               |
| 思考型预设沿用统一小输出额度容易无有效 JSON  | 各服务的推理开关和输出额度不同                     | 仅对已知域名及预设 ID 设置相应参数；DeepSeek/GLM 关闭思考，千问/硅基流动设置 enable_thinking=false；Kimi K3 使用 low 推理及 4096 输出上限 |
| Kimi K3 多轮请求不具备供应商要求的历史推理块 | 项目只保存可见问答，不保存供应商 reasoning_content | 对该预设把历史可见问答整理为用户上下文，保留系统指令，避免构造缺少推理信息的 assistant 消息                                               |
| 改完配置还显示上次测试成功                   | 测试结果没有与表单编辑关联                         | 编辑配置时清除旧结果，测试/保存期间禁用配置控件                                                                                           |

## 代码位置

- `src/constants/aiProviders.js`：六类目录、预设识别、密钥复用判定。
- `src/components/business/AiSettingsPanel.vue`：服务商选择、模型选择、自定义表单、控制台链接、状态反馈。
- `hotel-back/src/main/java/com/school/hotel/service/AiSettingsService.java`：密钥复用边界。
- `hotel-back/src/main/java/com/school/hotel/service/AiModelClient.java`：预设请求参数和脱敏 HTTP 错误映射。
- `tests/aiProviders.test.js`、后端 `AiProviderRequestTests`、`AiServicesTests`、`AiModelClientTests`：相关回归。
- `scripts/ai_presets_test.py`：浏览器验收。

## 实际验证

- 前端 ESLint、生产构建、修改文件 Prettier 检查通过；前端单元测试 29 项通过。
- 后端 AiServicesTests、AiModelClientTests、AiProviderRequestTests、AiSecurityTests 共 13 项通过，0 失败、0 错误、0 跳过。后端凭证持久化使用临时目录，模型响应使用本地 HTTP 测试服务器。
- 浏览器通过真实管理员登录进入页面；AI 设置读取、保存和测试响应使用受控拦截，不修改真实 AI 配置、不发送外部推理请求。
- 验证六类预设地址和默认 ID、缺少密钥拦截、切换清空、相同接口空密钥复用、保存重载、自定义重载、加载期间禁用、余额错误提示与旧结果清除。
- 1440/768/390/360px 下无横向溢出；桌面与手机截图已审视，浏览器运行时异常 0。
- 浏览器测试初次对 fieldset 使用 to_be_disabled 不被 Playwright 判定，改为检查真正的输入框及选择按钮，回归通过。这是测试断言修正，不是页面业务故障。

证据：`test-artifacts/ai-presets-20261119/results.json`、`desktop.png`、`mobile.png`。证据不保存密码、令牌或真实 API Key。

## 生效与验证边界

前端刷新即可查看；后端参数适配和凭证隔离需要重启当前后端/重新部署。未自动重启用户正在使用的 IDEA 服务。没有修改用户已有 AI 配置、数据库或密钥。

尚未使用用户各服务商的真实 Key 逐一调用，因此本次验收证明配置流程与代码适配，不代表所有外部账号已连接成功。实际使用：选择服务商 → 填写对应 API Key → 勾选启用 → 测试连接 → 保存配置 → 刷新首页。

## 官方依据（2026-11-19 核对）

- [DeepSeek 接口与模型](https://api-docs.deepseek.com/en/)、[思考参数](https://api-docs.deepseek.com/guides/thinking_mode/)
- [阿里云百炼 OpenAI 兼容接口](https://help.aliyun.com/zh/model-studio/compatibility-of-openai-with-dashscope)：明确旧 dashscope 域名仍可使用。
- [Kimi 模型列表](https://platform.kimi.com/docs/models)、[思考协议](https://platform.kimi.com/docs/guide/use-thinking-models)
- [智谱对话补全](https://docs.bigmodel.cn/api-reference/模型-api/对话补全)
- [硅基流动文本生成](https://docs.siliconflow.cn/docs/userguide/capabilities/text-generation)、[请求参数](https://docs.siliconflow.cn/docs/api/chat-completions-post)
- [OpenAI GPT-4.1 mini](https://developers.openai.com/api/docs/models/gpt-4.1-mini)

## 2026-11-19 控制台界面升级

管理员接入页由平铺表单升级为主流云服务控制台结构：左侧为服务商目录，右侧为配置工作区；顶部增加三步接入进度和实时连接状态。各服务商使用独立识别标记、说明和官方控制台入口，自定义服务仍保留完整地址与模型 ID 输入能力。

访问凭证区域新增密钥显隐、安全说明、已保存密钥状态和清除操作。测试连接与保存操作统一放在工作区底部，服务商切换时继续执行原有密钥隔离规则。界面没有引入新依赖，也没有修改后端接口契约。

本次实际验证：ESLint 通过；前端 31 项单元测试通过；Vite 生产构建通过；浏览器受控响应完成六个预设、自定义服务、保存重载、密钥隔离、忙碌态与错误态验证；1440、768、390、360、320px 均无横向溢出，页面运行时异常为 0。截图与结构化结果保存在 `test-artifacts/ai-presets-20261119/`。

浏览器验证过程中发现并修复两项交互兼容问题：服务商增加字母标记后，测试定位不能再假设可访问名称以服务商名开头；自定义开关的视觉滑块会覆盖原生复选框的自动化点击点，回归改为点击真实标签区域并校验勾选结果。
