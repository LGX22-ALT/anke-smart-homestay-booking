# 商家私信页面与头像展示修复

日期：2026-11-19。范围：独立住客消息中心、详情私信跳转、商家回复页面、首页与详情商家头像。源码和隔离环境已完成；线上未部署。

## 用户流程与实现

- 点击民宿详情「私信商家」进入 `/messages/房源ID`。游客先登录，跳转参数保留目标聊天。账户菜单和个人中心新增「我的私信」，可从 `/messages` 重新打开已有会话。
- 桌面居中左右分栏，手机显示列表或当前会话，可返回列表。会话有房源、头像、最近消息和未读数；消息显示时间、自己的发送/已读状态。可查看原房源。
- 复用既有 `tb_private_message` 和住客/商家消息 API，补充 `GET /api/messages` 返回当前住客的会话列表。按登录身份筛选，客户端不能指定他人住客 ID。商家侧沿用原房源归属校验。
- 住客和商家共用 `MessageInbox.vue`，每 5 秒获取更新；后台页签暂停请求，卸载清理监听/定时器；手机返回列表后只更新摘要，不调用会把消息标读的线程接口。
- 发送中阻止重复提交，失败原位提示并保留草稿；会话草稿保存在当前页面内存，切换时互不覆盖。输入支持换行及 Ctrl/Command+Enter，中文输入法确认不误发。
- 无 WebSocket、附件发送、系统推送或在线状态承诺。草稿不跨刷新保存，聊天记录真实落库。

## 确认的问题、原因与处理

| Bug | 原因 | 解决方式 |
| --- | --- | --- |
| 商家保存头像后，民宿页面仍不显示 | `HotelService.publicMerchant()` 没返回头像及个人名称，详情固定显示公司首字；首页列表没有商家资料和头像渲染 | 详情返回当前用户的公开头像/名称，列表按页批量关联商家和用户，首页与详情复用 `UserAvatar` |
| 手机聊天初次加载失败时无法重试 | 错误条位于手机默认隐藏的聊天窗，列表没有错误入口 | 会话列表单独展示错误与重试；模拟失败后实际点击恢复已通过 |
| 手机返回会话列表后新回复提前已读 | 定时刷新仍读取选中线程，而原线程接口会标记已读 | 依据断点和当前可见窗口决定是否读取线程，列表态仅更新未读摘要；实际双端回复验证通过 |

头像问题通过源码追踪和先失败的后端回归确认：原实现缺少公开字段；修改后默认头像能从资料保存进入列表/详情。浏览器再以真实 PNG 上传并保存，检查两处图片 URL 和实际加载，未用静态假头像代替测试。

私信原功能以详情弹窗实现，本次按用户要求改为独立页面，不把这个产品调整列为既有 Bug。上述两项手机问题在实现复核中发现，补充了对应浏览器用例并修复。另复现了手机切换会话加载失败时选中项被清空、提示再次隐藏的边界；现已保证聊天态始终显示错误和重试入口，新增对应失败到通过的回归。

## 实际验证

- 后端全量 `mvnw.cmd package`：106 项，104 通过，2 跳过，0 失败/错误。跳过的是原有完整预订浏览器流程和独立资料浏览器流程；本轮新商家聊天浏览器测试实际执行。
- 前端现有 31 项测试通过，ESLint 通过，生产构建通过。
- 新增隔离 H2 数据库 API 回归：资料保存后列表/详情头像与名称同步、不泄露密码/原始手机号、双向消息持久化和已读、不同住客会话隔离、不同商家越权拒绝、游客拒绝、空白/超长消息及下架房源拒绝。
- 真实浏览器 12 类场景：上传头像/名称、首页与详情展示、私信目标跳转、发送和刷新持久、输入法保护、商家接收并回复、失败发送重试、五个尺寸和手机返回、列表保持未读、消息中心重新进入、加载错误恢复、会话切换与草稿隔离、切换路由失败重试、游客登录跳转。尺寸为 1440、1024、768、390、360；尺寸检查合为一类，多个操作合并记入 12 项场景。
- 浏览器使用真实隔离后端，仅「发送失败」与「列表加载失败」两条异常分支由浏览器故障注入。无运行时页面错误。截图与响应记录不包含登录令牌、Cookie 或密钥。
- 首轮浏览器上传遇到 403，确认是隔离测试服务未放行专用 Vite 端口的 Origin；仅调整测试配置，保留生产 CORS。Chrome DevTools 在先前任务中有环境限制，本轮使用现有 Playwright 验证。

## 五阶段界面检查

UIUX 检索命中输入法 Enter 防误发规则并应用；最初设计系统结果偏营销演示，未采用其首页布局和配色。Frontend Design 沿用项目紫色和居中工作区；Taste 控制边距、信息密度与文字换行；GSAP 阶段无需新增动效或库，滚动即时定位且不打断向上阅读；Impeccable 沿用既有启动器不可用记录，以实际截图和交互完成 polish，不声称专用引擎通过。

## 改动位置与作用

| 文件 | 作用 |
| --- | --- |
| [hotel-back/src/main/java/com/school/hotel/entity/Hotel.java](D:/overword/hotel-back/src/main/java/com/school/hotel/entity/Hotel.java) | 非数据库字段 `merchant` 承载安全公开房东资料，不新增表字段 |
| [hotel-back/src/main/java/com/school/hotel/service/HotelService.java](D:/overword/hotel-back/src/main/java/com/school/hotel/service/HotelService.java) | `publicMerchant` 返回真实头像/名称，`attachPublicMerchants` 按页批量填充列表，避免首页逐房源查询 |
| [hotel-back/src/main/java/com/school/hotel/controller/MessageController.java](D:/overword/hotel-back/src/main/java/com/school/hotel/controller/MessageController.java) | 增加登录住客会话列表入口 |
| [hotel-back/src/main/java/com/school/hotel/service/MessageService.java](D:/overword/hotel-back/src/main/java/com/school/hotel/service/MessageService.java) | 当前住客会话分组、头像、摘要、未读与不可咨询状态；时间相同时以 ID 稳定排序 |
| [hotel-out/src/api/modules/message.js](D:/overword/hotel-out/src/api/modules/message.js)、[src/services/dataSource.js](D:/overword/hotel-out/src/services/dataSource.js) | 接入住客会话列表，保留原消息发送及商家回复接口 |
| [hotel-out/src/components/business/MessageInbox.vue](D:/overword/hotel-out/src/components/business/MessageInbox.vue) | 双端共享聊天、轮询、未读、草稿、异常、头像和响应式界面 |
| [hotel-out/src/views/client/Messages/index.vue](D:/overword/hotel-out/src/views/client/Messages/index.vue)、[src/router/modules/client.js](D:/overword/hotel-out/src/router/modules/client.js) | 新消息中心页面及受住客权限保护的路由 |
| [hotel-out/src/views/client/HotelDetail/index.vue](D:/overword/hotel-out/src/views/client/HotelDetail/index.vue) | 弹窗替换为聊天路由跳转，真实商家头像及个人名称展示 |
| [hotel-out/src/views/client/Home/index.vue](D:/overword/hotel-out/src/views/client/Home/index.vue) | 住宿卡片新增真实房东头像与名称 |
| [hotel-out/src/views/merchant/MessageManage/index.vue](D:/overword/hotel-out/src/views/merchant/MessageManage/index.vue) | 商家后台复用完整聊天界面 |
| [hotel-out/src/components/common/NavHeader.vue](D:/overword/hotel-out/src/components/common/NavHeader.vue)、[src/views/client/UserCenter/index.vue](D:/overword/hotel-out/src/views/client/UserCenter/index.vue) | 我的私信入口，商家账户菜单指向商家消息页 |
| [hotel-back/src/test/java/com/school/hotel/service/MerchantChatTests.java](D:/overword/hotel-back/src/test/java/com/school/hotel/service/MerchantChatTests.java) | 隔离数据库业务/权限回归及浏览器后端启动 |
| [hotel-out/scripts/merchant_chat_test.py](D:/overword/hotel-out/scripts/merchant_chat_test.py) | 真实 HTTP 头像上传和双向聊天浏览器回归、截图与异常注入 |
| [hotel-out/DESIGN.md](D:/overword/hotel-out/DESIGN.md)、本报告及同日 `merchant-chat-plan.md` | 设计决策、计划、回归与交付边界 |

## 证据

此外，`hotel-back/AI_PRODUCTION_UPDATE.md` 追加了同日 JAR 更新说明，避免旧校验值被用于核对本轮新产物。生产静态包与产物校验 JSON 是本轮生成的交付文件。

- [test-artifacts/merchant-chat-red.log](D:/overword/hotel-out/test-artifacts/merchant-chat-red.log)：修复前失败。
- [test-artifacts/merchant-chat-green.log](D:/overword/hotel-out/test-artifacts/merchant-chat-green.log)：最小后端回归通过。
- [test-artifacts/merchant-chat-backend.log](D:/overword/hotel-out/test-artifacts/merchant-chat-backend.log)：后端全量构建与测试；[merchant-chat-final-integration.log](D:/overword/hotel-out/test-artifacts/merchant-chat-final-integration.log)：最后一轮 4 项专项与 12 场景回归；[merchant-chat-switch-red.log](D:/overword/hotel-out/test-artifacts/merchant-chat-switch-red.log)：切换错误隐藏修复前失败。
- [test-artifacts/merchant-chat-browser.log](D:/overword/hotel-out/test-artifacts/merchant-chat-browser.log)、`merchant-chat/results.json`：浏览器场景结果。
- [test-artifacts/merchant-chat/chat-1440.png](D:/overword/hotel-out/test-artifacts/merchant-chat/chat-1440.png)、`chat-390.png` 等：桌面及手机截图。
- [test-artifacts/merchant-chat-unit.log](D:/overword/hotel-out/test-artifacts/merchant-chat-unit.log)、[merchant-chat-lint.log](D:/overword/hotel-out/test-artifacts/merchant-chat-lint.log)、[merchant-chat-build.log](D:/overword/hotel-out/test-artifacts/merchant-chat-build.log)：前端检查。

## 部署产物

- 前端：[hotel-out/release/hotel-chat-frontend.zip](D:/overword/hotel-out/release/hotel-chat-frontend.zip)，仅包含生产构建静态文件，解压内容直接放入站点目录。
- 后端：[hotel-back/target/hotel-back-0.1.0.jar](D:/overword/hotel-back/target/hotel-back-0.1.0.jar)，包含当前头像和私信接口。
- 校验：[test-artifacts/merchant-chat-artifact.json](D:/overword/hotel-out/test-artifacts/merchant-chat-artifact.json)。不要再用此前 AI 排查时的旧产物校验值核对本轮新 JAR。

## 部署与未验证项

本地 IDE 的现有 8081 进程未被结束；使用当前开发页面前需要在 IDE 重启后端加载新增接口。生产需要同步更新前端 `dist` 和配套后端 JAR，并重启实际 Java 服务；仅更新前端会再次出现接口版本不配套。

本轮无新数据库字段或表。原项目已含 `tb_private_message` 及 `V003__add_hotel_nearby_and_private_message.sql`；若很早的线上数据库尚未执行既有迁移，应先确认该表存在，不能导入会清空数据的完整初始化 SQL。

未操作线上数据库、未向真实商家发送私信。未验证真实手机硬件、系统键盘弹起、线上部署和多实例消息推送。所有真实发送都在临时测试用户和隔离 H2 数据库进行。
