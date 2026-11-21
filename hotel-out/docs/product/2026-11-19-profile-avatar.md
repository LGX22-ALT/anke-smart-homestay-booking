# 用户与商家个人资料交付记录

日期：2026-11-19。状态：实现完成，隔离环境验证通过；当前由 IDE 运行的本地后端需要重启后加载新接口。

## 功能与入口

- 用户：个人中心 → 个人信息（`/user/profile`）。保留原有手机号编辑与认证状态展示。
- 商家：左侧个人资料，或点击右上角头像/名称（`/merchant/profile`）。
- 两端可修改个人展示名称，支持 1–64 个字符、去除首尾空格；可预览并上传 JPG/PNG（最大 5MB），也可选择六款内置头像后保存。
- 保存后页头和个人中心同步；刷新和重新登录从服务端读取已保存资料。登录账号、角色、店铺名称和订单规则保持原有含义。
- 上传图片校验实际内容、尺寸（单边不超过 4096、总像素不超过 1600 万），中心裁切为最大 512px 的 PNG；默认头像由站内静态资源提供，不调用外部头像服务。

## 确认的问题与修复

| Bug | 原因 | 解决方式 |
| --- | --- | --- |
| 用户无法上传或快速选择头像 | 个人信息表单只有名称、手机号，页头使用固定图标 | 加入共享编辑器、上传预览与六款默认头像；保存后同步 Pinia 和持久会话资料 |
| 商家没有个人资料编辑入口 | 商家路由与侧栏没有对应页面 | 新增个人资料页，侧栏及右上角均可进入，复用用户编辑器与认证接口 |
| 局部修改返回的头像、认证状态不完整 | 未提交字段被赋空后直接用内存实体生成响应 | 保留未提交字段，保存后重新查询返回资料 |
| 并发修改名称与头像可能互相覆盖 | 完整旧实体提交会把另一请求的新字段写回旧值 | 仅更新请求提供的字段；屏障测试先确认丢失更新，再验证两项同时保留 |
| 名称可保存为空白且没有长度限制 | 原接口未校验名称 | 前后端限制长度，服务端去除首尾空格并拒绝空白名称 |

复现：旧用户个人信息页没有头像控件，商家侧栏没有个人资料；调用旧资料接口仅提交名称时，返回头像为空、认证状态被误报。并发测试控制两个请求先读取同一旧记录，再分别修改名称和头像，旧实现出现头像恢复旧值；修复后数据库保留两项修改。

## 改动位置

前端：

- `src/components/business/ProfileEditor.vue`：加载、预览、默认头像、校验、保存、失败重试、状态提示、预览释放。
- `src/components/common/UserAvatar.vue`、`src/constants/avatars.js`、`public/avatars/*.svg`：统一头像显示、六款头像和加载失败回退。
- `src/views/client/UserCenter/index.vue`、`src/views/merchant/Profile/index.vue`、`src/router/modules/merchant.js`：两角色资料入口；手机导航完整显示。
- `src/components/common/NavHeader.vue`、`src/components/layout/BackOfficeLayout.vue`、`MerchantLayout.vue`：保存后显示头像与名称。
- `src/stores/modules/user.js`：只更新当前账号资料，保留当前认证令牌，拒绝其他会话的迟到结果。
- `src/api/modules/user.js`、`src/services/dataSource.js`、`src/utils/profileValidation.js`：上传/保存、FormData 请求、演示模式兼容、前端校验。

后端：

- `controller/UserController.java`、`service/UserService.java`：认证的 `/api/user/avatar` 上传接口、局部资料更新、名称校验。
- `service/AvatarStorageService.java`：内容/格式/大小/尺寸校验、正方形图片存储、头像路径校验。
- `dto/ProfileUpdateRequest.java`：名称长度上限。

没有新增数据库字段或迁移。

## 实际验证

- 前端 `npm test`：31 项通过，包含名称与文件校验；`npm run lint` 和 `npm run build` 通过。
- 后端 `mvnw -DskipTests package` 打包通过，已核对 JAR 包包含头像存储服务。
- 后端 Maven 全套：98 项，97 通过、0 失败、0 错误、1 项跳过。跳过的是未设置 `RUN_BROWSER_TESTS` 的既有完整订单浏览器回归。本轮 `RUN_PROFILE_BROWSER_TESTS=true` 的头像专项浏览器测试已执行。
- 新增 `ProfileAvatarTests` 11 项全部通过：两角色保存与再次登录、局部字段保留、名称和头像路径校验、真实 PNG/JPG 上传、空/伪造/超大/过宽图片、未认证拒绝、账号隔离、并发局部更新和浏览器流程。
- 浏览器实际操作：用户和商家均完成选择默认头像、保存名称、真实上传、模拟保存失败后重试、页头同步、刷新、退出浏览器会话后重新登录；1440/1024/768/390/360px 无文档横向溢出，360px 六个用户导航入口完整可见，无页面运行错误。
- 既有离线演示模式：两角色名称和默认头像保存、刷新、页头同步通过；演示模式不支持真实上传，使用默认头像。
- 两次只读审查：首次发现并发覆盖问题；修复和测试补齐后复查未发现阻断项。期间发现控制器新增依赖影响原 MVC 切片测试，改回由原用户服务统一委托；最终登录/注册/CORS 回归通过。

## 证据

- `test-artifacts/profile-avatar-browser.log`：两角色专项浏览器结论。
- `test-artifacts/profile-avatar/`：`user-desktop.png`、`user-mobile.png`、`merchant-desktop.png`、`merchant-mobile.png` 及默认头像选择截图。上传测试图片为纯色测试素材，便于确认保存和请求链路。
- `test-artifacts/profile-backend-summary.json`：完整后端测试计数与各套件结果。
- `test-artifacts/profile-build.log`：前端构建记录。
- 工作区 `.codex-artifacts/profile-backend-final.log`：后端最终测试记录；`profile-concurrency-red.log`：并发缺陷修复前的失败证据。

测试仅使用独立 H2 数据库与测试账号；未更改已有用户或商家资料。没有将登录令牌、Cookie 或密码写入报告和截图。早期失败日志中的认证头内容已清理。

## 设计流程与运行边界

按 UIUX Pro Max → Frontend Design → Taste → GSAP → Impeccable 顺序核对，沿用 `DESIGN.md` 的紫色品牌、系统字体、44px 操作区域与操作型布局；不增加装饰动画。Impeccable 沿用已记录的启动器限制，以项目规范、实际浏览器和截图检查验收，未宣称专用引擎通过。

本次源码与隔离测试环境已加载新增接口；8081 的现有进程由 IDE 启动，未结束其调试会话，需要在 IDE 重启后端后使用真实头像上传。生产部署及 MySQL 的资料写入未在本轮执行；无需数据库迁移。
