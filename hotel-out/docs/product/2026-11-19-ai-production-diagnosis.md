# 线上 AI 不可用排查

日期：2026-11-19。状态：用户已确认网址及「管理后台保存 AI 配置」的触发入口。已确认线上新版前端与旧版后端的连接管理接口不配套，且 AI 当前禁用。配套后端已重新构建，线上更新与保存回归尚未执行。

## 后续核对：部署版本不配套

1. 用户确认网址为 `http://blog.liangguoxi.icu/`，异常发生在后台保存 AI 配置时。
2. 线上 `/assets/router-DT5rr14a.js` 包含 `/admin/ai/connections`、编辑、激活与测试路径，并加载 `AiConnectionLibrary-Bb7cTjdW.js`。本地 `AiConnectionLibrary.vue` 的 `save()` 通过 `saveAiConnection()` 调用这些接口。
3. 通过记录中的后端地址 `http://103.212.186.243:8081/v3/api-docs` 核对，线上只有 `/api/admin/ai/settings`（GET/PUT）、`/api/admin/ai/test`（POST）及两个公开 AI 接口，没有任何 `/api/admin/ai/connections` 接口。新版保存请求无法由当前旧后端处理。
4. 本地 `AdminAiController.java` 已有完整连接管理接口，无需重写该功能。本轮执行 `mvnw.cmd package` 构建成功，100 项测试通过、2 项浏览器测试因环境条件跳过，0 失败、0 错误；AI 相关 17 项测试全部通过。生成 JAR 的控制器字节码已确认包含缺失接口。
5. 独立 Playwright 浏览器访问真实网站：首页 HTTP 200，`/api/ai/status` 业务 200、`enabled:false`。Chrome DevTools 导航遭遇 `ERR_BLOCKED_BY_CLIENT`，已改用独立浏览器复核，没有将工具限制误判为站点故障。

发现方式：对比线上前端请求和后端接口契约，结合用户确认的保存入口。主要难点是公开状态只能看到开关；后续接口契约对比确认了部署问题。未以管理员身份提交生产配置，具体保存请求的错误码与异常堆栈仍未抓取，不能声称已在线复现认证后的错误响应。

解决方式：替换并重启实际运行的后端为本轮配套 JAR，再在后台保存并启用/激活连接。操作及验收步骤见 [后端生产更新说明](../../../hotel-back/AI_PRODUCTION_UPDATE.md)。第三方密钥有效性与网络连通性仍要在后端更新后验证。

证据位于 `test-artifacts/`：`ai-production-version-check.json`（线上接口差异）、`ai-production-browser.json`（真实浏览器状态）、`ai-production-build-summary.json`（测试统计）、`ai-production-artifact.json`（JAR 校验与接口标记）。下面保留首次公开接口排查记录。

## 实测

根据项目既有部署记录检查 `http://blog.liangguoxi.icu`，不使用管理员凭证、不读取密钥：

- `GET /api/hotels?pageNum=1&pageSize=1`：HTTP 200，业务 code 200，总数 51，表明站点 API 代理和酒店列表可用。
- `GET /api/ai/status`：HTTP 200，业务 code 200，`enabled:false`。
- `POST /api/ai/search`：提交短查询与 quick 模式，HTTP 200、业务 code 1001，提示“AI 助手尚未启用，可以先使用酒店搜索”。因为 AI 禁用，未触发付费模型调用。
- 直接访问记录中的云端 8081，AI 同样 disabled。IP 的默认 80 站点返回 404，不据此判断域名站点故障。

证据：`test-artifacts/ai-production-diagnosis.json`，仅含公开响应状态，无凭证。

## 根因与边界

已确认导致此实例 AI 搜索不可用的直接条件：后端 AI 未启用。尚不能判断是未配置、连接停用、未激活，还是部署后读取了不同配置文件。

`hotel-back/src/main/java/com/school/hotel/service/AiSettingsService.java` 构造方法从 `hotel.ai.config-path` 加载配置；默认 `./data/ai-settings.json`，相对于 Java 进程工作目录。文件不存在时默认 disabled。配置不在数据库，也不会通过前端 dist 迁移；本地配置可用不代表服务器拥有该配置。路径和文件丢失属于待核实原因，不能当作已复现根因。

`AiController.status()` 仅返回开关；`AiSearchService.search()` 在 disabled 时直接拒绝，当前实测符合这条分支。公开接口返回了明确未启用信息，没有复现 HTTP 500 或服务器内部错误。因此不能宣称已定位管理员测试连接异常，也不能宣称线上已修复。

## 处理步骤

1. 在线上管理员页面 `/admin/ai` 配置或选择现有连接，填写正确接口地址、服务商提供的模型 ID 及服务器可用的密钥。
2. 测试连接成功后启用并保存连接，设为当前激活连接。单独测试连接不会自动启用首页 AI。
3. 刷新 `/api/ai/status`，确认 `enabled:true`，再验证首页提问。
4. 若原已保存过而部署/重启后变为禁用，在启动环境明确设置 `HOTEL_AI_CONFIG_PATH` 为后端专用持久目录内的绝对路径，确认运行用户读写权限。不要放在前端静态目录，不覆盖其他配置。
5. 如果线上管理员测试连接本身报服务器异常，需要该时刻 `/api/admin/ai/.../test` 的状态、脱敏错误文字及后端异常日志判断；不索取 API Key、Cookie 或服务器密码。

难点：公开状态接口只能确认启用开关，无法证明配置文件的路径、权限或第三方网络/密钥有效性。当前没有服务器登录及管理端请求证据，故保留上述原因待核实，没有盲改代码或配置。

## 本轮改动

更新本排查记录和公开接口证据，新增版本、浏览器、测试和产物证据；更新 `hotel-back/AI_SETUP.md` 的新版连接管理步骤及接口清单，新增 `hotel-back/AI_PRODUCTION_UPDATE.md` 说明宝塔更新、校验和验收方法。生成 `hotel-back/target/hotel-back-0.1.0.jar`，未修改产品源码、Nginx 或线上模型配置。未进行密钥有效性、余额、服务器到模型提供商网络或真实生成验证。
