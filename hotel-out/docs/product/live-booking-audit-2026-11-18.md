# 线上预订入口故障排查与修复

日期：2026-11-18。站点：<http://blog.liangguoxi.icu/>。遵循 `product-delivery-agent` 流程。

**状态：线上故障已复现；三项前端问题已在本地修复并验证，修复包已生成；尚未部署到云端。HTTPS 未修复。** 用户选择 SSH 部署并提供密钥，但网站 IP 上的 `root` / 22 端口拒绝该密钥认证；已请求核对连接信息和服务器公钥授权。未取得远程 shell，未修改线上文件。

## 按风险排列的确认问题

| Bug                                                                 | 原因                                                                                                                       | 解决方式                                                                                                                                                                                            |
| ------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| P1 高风险：有库存的房型点击预订后无响应，阻断预订入口               | `setBooking()` 直接调用 `crypto.randomUUID()`；公网 HTTP 页面不是安全上下文，该方法不存在，异常发生在跳转登录/订单确认之前 | 新增 `createRequestId()`：支持时调用原生方法，否则使用 `crypto.getRandomValues()` 生成符合 UUID v4 格式的请求号。继续为每次房型选择生成新请求号，同一次提交重试保持不变。本地修复及回归通过，待部署 |
| P1 高风险：正式登录页面仍显示三种演示角色的快捷入口并预填账号、密码 | 演示入口未受 Mock 模式限制，并会使用写在前端的演示凭据调用真实登录接口。生产默认账号是否仍有效，本次没有登录验证           | 正式页面清空预填字段，演示按钮和提示仅在 Mock 模式显示，移除演示按钮调用真实登录接口的分支。本地修复及回归通过，待部署；服务器存量演示账号须另行核查、禁用或轮换凭据                                |
| P2 中风险：无封面图片的酒店在订单确认页显示破图（隔离测试环境复现） | 确认页无条件渲染 img，没有判断图片地址为空或加载失败                                                                       | 增加缺图及加载失败占位，沿用搜索页的文案和配色；HTTP 构建回归检查占位可见、破图元素不存在。待部署                                                                                                   |

未修复、独立的上线风险：**P1：HTTPS 不可达**。直接连接 `https://blog.liangguoxi.icu/` 被拒绝，HTTP 页面仍可访问。不能仅由此判定是未监听 443、安全组/防火墙还是证书配置问题。HTTP 登录和个人信息传输缺少 TLS 保护。

解决顺序：宝塔为正确域名申请并部署有效证书 → 开启站点 443 监听并检查云安全组/系统防火墙 → 检查并重载 Nginx → 验证 HTTPS 页面及同源 API → 再启用 HTTP 到 HTTPS 跳转。保留现有 `/api/`、`/uploads/` 代理和 Vue 回退。当前没有云端访问通道，该项未执行、未验证；前端兼容修复不能替代 TLS。

## 复现与根因证据

1. 使用本机真实 Chrome 直接打开线上首页、搜索、景点、登录、`/hotel/1`，均成功渲染，HTTP 200。内置 DevTools 打开域名受到工具侧 `ERR_BLOCKED_BY_CLIENT` 限制，因此改用项目已有 Playwright 驱动本机 Chrome 检查，并未把工具错误算作网站故障。
2. 详情默认查询 2026-11-19 至 2026-11-20，房态接口 HTTP 200 / 业务 code 200，房型 1、3、4 分别有 4、3、2 间可订库存。
3. 点击第一个“预订此房型”，地址仍为 `/hotel/1`，未进入登录页。Console 实际报错：`TypeError: crypto.randomUUID is not a function`，堆栈指向线上 `router-jfVF2wm2.js` 的预订 store，再返回详情按钮处理函数。
4. 同页读取环境：`isSecureContext=false`、`typeof crypto.randomUUID='undefined'`、`typeof crypto.getRandomValues='function'`。这解释了为什么本地 localhost 能运行，而上线公网 HTTP 后失败。
5. 线上登录页检测到 3 个演示入口，手机号和密码字段均非空；证据只保存是否预填的布尔值，未保存字段值。未使用演示账号登录线上，不能声称已经证明可以进入真实管理后台。
6. 检查隔离环境订单确认页截图时，发现无酒店封面时显示破图；新增浏览器断言确认旧构建不存在缺图占位，随后修复。该问题在有效的缺图测试数据下复现，没有声称线上酒店 1 的封面也缺失。证据：[修复前截图](../../test-artifacts/live-booking/missing-image-before.png)、[失败断言](../../test-artifacts/live-booking/missing-image-red-browser.log)、[修复后确认页](../../test-artifacts/live-booking/http-booking-confirm.png)。

原始证据：

- [线上浏览器检查和错误堆栈](../../test-artifacts/live-booking/browser-before.json)
- [线上预订无跳转截图](../../test-artifacts/live-booking/production-reserve.png)
- [公开接口、权限边界、HTTPS 检查](../../test-artifacts/live-booking/public-api.json)
- 可重复执行的只读线上检查：[live-booking-audit.js](../../scripts/live-booking-audit.js)。不会登录或提交订单；每次执行会更新浏览器证据文件。

## 改动位置与实现

- [requestId.js](../../src/utils/requestId.js)：使用 16 字节加密随机数，设置 UUID v4 版本位和变体位，按 UUID 标准格式输出；没有降级为 `Math.random()`。
- [booking.js](../../src/stores/modules/booking.js)：统一调用兼容的请求号生成方法，其余预订信息和幂等重试行为保持一致。
- [登录页面](../../src/views/client/Login/index.vue)：清空初始账号和密码，Mock 才显示演示入口；正常登录仍由用户填写凭据调用真实接口。
- [订单确认页](../../src/views/client/OrderConfirm/index.vue)：缺少图片地址或图片加载失败时显示占位，保留摘要布局。
- [booking.test.js](../../tests/booking.test.js)：对真实 Pinia store 验证无 `randomUUID` 时的预订、请求号格式、重新选择房型换号及清理状态，并覆盖原生方法分支。
- [production-http.spec.js](../../e2e/production-http.spec.js)：从 `dist` 读取实际生产构建，在 `http://booking.test` 非安全上下文测试未登录预订 → 正式登录 → 订单确认，断言表单无预填凭据、无演示入口、可提交且保留正确房型。
- `hotel-back/src/test/java/com/school/hotel/service/BrowserJourneyTests.java`：仅给隔离浏览器测试增加 `http://booking.test` 来源白名单，没有修改生产跨域策略和后端业务逻辑。

## 实际验证结果

| 检查                                   | 实际结果                                                                                                                                                    | 证据                                                                                                   |
| -------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| 新增预订回归测试修复前                 | 1 失败、1 通过；失败原因为同样的 `crypto.randomUUID is not a function`                                                                                      | [unit-red.log](../../test-artifacts/live-booking/unit-red.log)                                         |
| 正式登录页回归修复前                   | 新测试要求空账号字段时失败，旧生产构建仍预填演示账号；原有 5 条浏览器流程通过                                                                               | [production-login-red-browser.log](../../test-artifacts/live-booking/production-login-red-browser.log) |
| `npm test`                             | 27/27 通过，退出码 0                                                                                                                                        | [unit-final.log](../../test-artifacts/live-booking/unit-final.log)                                     |
| `npm run lint`                         | 通过，退出码 0                                                                                                                                              | [lint.log](../../test-artifacts/live-booking/lint.log)                                                 |
| `npm run build`                        | 生产构建成功，退出码 0                                                                                                                                      | [build.log](../../test-artifacts/live-booking/build.log)                                               |
| Java 17 Maven 全量测试，启用浏览器回归 | 83 项，失败 0、错误 0、跳过 0，BUILD SUCCESS                                                                                                                | [backend-and-browser-final.log](../../test-artifacts/live-booking/backend-and-browser-final.log)       |
| Playwright 浏览器回归                  | 6/6 通过：异常房源数据、切换账号迟到响应、下单到退款、搜索重试/移动端、过期订单/订单归属、生产构建 HTTP 预订入口                                            | [browser-final.log](../../test-artifacts/live-booking/browser-final.log)                               |
| 线上只读检查                           | 首页/搜索/景点/详情/登录 HTTP 200；列表/房态业务成功；缺失静态资源 404；缺失/错误/过去日期业务 400；不存在房源业务 404；未登录订单/商家/管理员 API HTTP 401 | [public-api.json](../../test-artifacts/live-booking/public-api.json)                                   |
| 线上移动端 390px                       | 搜索、详情、登录页文档宽度均为 390，无水平溢出；不代表所有弹窗和交互都已覆盖                                                                                | [browser-before.json](../../test-artifacts/live-booking/browser-before.json)                           |
| ZIP 完整性和文件一致性                 | CRC 校验通过，114 个静态文件，与已测试的 dist 逐文件一致，根目录直接有 index.html                                                                           | [package.json](../../test-artifacts/live-booking/package.json)                                         |

HTTP 构建回归使用隔离 H2 数据库和真实 Java 接口，通过浏览器路由提供静态构建和模拟代理传输；这证明修复构建在非安全 HTTP 上的业务行为，不代表云端 Nginx 已被更改。初次该测试的代理来源被测试后端拒绝（403），已仅补齐测试环境来源配置，最终全量复测通过。

本次没有在生产环境创建订单、支付、退款、注册、修改配置或操作业务数据。真实下单到退款流程只在隔离测试环境执行；线上账户登录和更新后线上预订均尚未验证。

## 修复包与宝塔更新

已生成 [hotel-frontend-fix-20261118.zip](../../release/hotel-frontend-fix-20261118.zip)，大小 5,653,318 字节，SHA-256：

`d9079d345daf1f9ce3c827d9cd28c833d22976eaaf055850d03e0484f3173de1`

这是本次修复包，旧 `hotel-frontend-baota.zip` 属于此前构建，不要误传。当前故障不要求更新后端 JAR 或数据库。

1. 宝塔文件管理中备份 `/www/hotel/hotel-ui` 现有静态文件，保留用于回滚。
2. 将新包上传并解压到临时目录，确认其根目录直接含 `index.html`、`assets/` 和 `attractions/`。
3. 先将新 `assets/` 和其他静态资源合并到 `/www/hotel/hotel-ui`，最后替换 `index.html`。保留旧哈希资源一段时间，避免已打开的页面加载旧模块失败。无需改动已正常工作的 `/api` 代理。
4. 强制刷新页面，如配置了 CDN 则刷新入口 HTML 缓存。核对新入口资源已更新，正式登录页无预填凭据和演示快捷入口。
5. 打开可订日期的房源详情，点击“预订此房型”，未登录应进入 `/login?redirect=/order/confirm`；使用自己的测试账号登录应进入 `/order/confirm`，房型/日期正确，Console 无 UUID 报错。线上提交订单需使用明确的测试账号和测试数据，本报告未执行。
6. 如更新后异常，恢复备份的匹配入口文件和静态资源；不要删除上传目录或数据库数据。

## 待处理和验证边界

- **待部署 / 线上未修复验证**：三个前端修复已验证并打包，但当前仍未上传云端。
- **SSH 阻塞**：当前网站 IP 为 `103.212.186.243`，22 端口可达；尝试 `root` 用户时，SSH 已识别并提交所提供密钥的公钥，但服务端仍返回 `Permission denied (publickey,gssapi-keyex,gssapi-with-mic,password)`。当前不能确定是用户名/目标服务器不匹配，还是公钥未授权或服务器认证策略限制。没有继续尝试密码或其他账号。密钥内容不写入本报告或项目证据。
- SSH 检查使用的临时私钥放在项目外，并已限制本机访问权限；认证失败后请求删除临时副本，但自动审批以 `blocked by policy` 拒绝了该操作，需用户手动删除 `C:/Users/lgx2/.ssh/hotel_deploy_temp_20261118`。未修改用户原有密钥文件。聊天中提供的私钥建议由服务器管理员撤销并更换。
- **待处理**：HTTPS 443 可达性、证书部署与强制 HTTPS；必须取得服务器有效配置后才能确定具体原因。
- **待核查**：生产演示账号是否仍有效及凭据轮换。隐藏演示入口不会撤销已有会话或自动修改服务器账号。
- **未验证**：真实生产登录、创建订单/支付/退款、生产 Redis 故障、上传持久化和全面安全审计。本次通过的权限检查是未登录只读请求，不代表完整越权审计通过。
