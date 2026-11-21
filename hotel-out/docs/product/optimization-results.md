# 优化与自检结果（2026-11-16）

对应用户授权：实施 audit-2026-11-16.md 中全部六项建议，并自检修复缺陷。沿用 product-delivery-agent，顺序实施，另做了一轮独立只读审查。现有业务流程和界面风格保留。

## 已落实的优化

| 项目                | 实施内容                                                                                                                                                                                    | 位置与验证                                                                                                    |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| 生产 Redis 故障策略 | `hotel.redis.required=true` 时缺失或操作失败返回业务 503，并记录不含键、令牌和负载的错误日志；不再静默退回进程内存。开发 profile 仍允许内存                                                 | TokenStore、TokenStoreTests；所有主要操作故障注入、缺失 provider、内存模式测试                                |
| 搜索查询效率        | 由数据库相关 EXISTS 筛选房型，日历按同一房型分组，统计不同日期数必须等于入住夜数。酒店条件保留在外层查询；人数、床型、日期和夜数均绑定参数。避免读取全量房型/日历到 Java 及构造大型 ID 列表 | HotelService、HotelSearchTests、BookingConsistencyTests；缺夜、跨房型、软删除、人数、城市及恶意床型字符串覆盖 |
| 无评价展示          | reviewCount 为 0 或缺失时显示“暂无评分”，有评价时显示分数                                                                                                                                   | Search/index.vue；浏览器断言                                                                                  |
| 加载与错误状态      | 每次搜索立即清除旧列表、总数、页数，结果区显示加载状态，保留失败重试及请求版本校验                                                                                                          | Search/index.vue；Playwright 延迟响应、503 后重试、390px viewport 布局断言                                    |
| 核心流程回归和 CI   | 新增 Playwright、隔离 H2 随机端口后端和专用测试用户；自动启动 Vite，测试结束由测试工具关闭。GitHub Actions 执行 npm ci、Node 测试、lint、build 和 Java/浏览器回归                           | BrowserJourneyTests、playwright.config.js、e2e/booking.spec.js、根目录 .github/workflows/checks.yml           |
| 固定依赖            | 全部直接依赖从已有 lockfile 解析具体版本；新增 Playwright 同样固定；同步 lockfile，并实际执行 npm ci                                                                                        | package.json、package-lock.json                                                                               |

## 自检发现并修复的缺陷

1. **同秒刷新令牌重复**：JWT 的 subject、role、type、签发/过期时间相同，连续生成的令牌完全一致。新增随机 jti，使每个刷新令牌唯一。新增测试修复前 10 次生成只有 1 个唯一值，修复后为 10。
2. **刷新令牌可并发复用**：之前读取与删除分离，两个请求均可能完成验证。新增原子轮换，同一个旧令牌仅允许一次成功。新增并发测试修复前两个请求成功，修复后只有一个。
3. **轮换中途故障可能损失会话**：独立复核发现“先消费旧值再保存新值”的失败窗口。最终采用 Redis Lua 在一条命令内验证旧值、写新值、删旧值；内存模式通过同步块实现。脚本命令抛错保持业务 503，不落入内存；测试验证命令故障后重试，不再使用独立删除。
4. **缺少封面显示破图**：在窄屏截图发现缺图房源的破图图标。搜索推荐和结果卡片均增加占位，图片加载失败同样切换占位，并用真实组件浏览器断言覆盖缺图。

测试脚本调试中还修正了登录按钮同名、预订按钮文案定位和测试端口 CORS 配置，均为测试环境问题，没有放宽生产 CORS。

## 验证与证据

- 完整 Java 回归：77 个测试，0 失败、0 错误、0 跳过；其中 BrowserJourneyTests 启动并断言真实浏览器子进程和最终数据库库存。
- 浏览器三个场景：住客登录下单支付 → 商家确认 → 住客申请退款 → 管理员确认退款 → 住客查看结果及访问管理员页面被拒绝；搜索加载/重试/暂无评分/窄屏；跨账号订单隔离和过期支付取消、重复操作不重复恢复库存。
- 前端 Node 测试：15 个通过；lint 与生产构建退出码 0。
- `npm ci` 成功。当前机器使用已安装的 Chrome 执行 Playwright，CI 配置下载 Chromium。
- 浏览器截图：`hotel-out/test-results/booking-search-loading-retry-rating-and-mobile-layout/search-mobile.png`。实际 viewport 为 390×844，断言页面无横向溢出。
- 日志：根目录 `.codex-artifacts/optimization-all.log`、`optimization-browser-final.log`、`optimization-playwright.log`、`optimization-frontend-tests.log`、`optimization-lint.log`、`optimization-build.log`；故障复现日志 `optimization-redis-red.log`、`optimization-refresh-red.log`。
- 失败自动保存截图、页面错误上下文及只包含路径/状态的网络诊断，关闭包含认证负载的 trace 保存。测试产物目录加入 gitignore。

## 验证边界与运行说明

- SQL 正确性、库存和流程使用 H2 MySQL 兼容模式验证；本轮没有实际 MySQL 大数据压测，不能给出 P95 或性能提升百分比。
- Redis 使用 Mockito 故障注入及内存并发回归，未执行真实双实例 Redis 断网演练。原子脚本已减少中间状态，但脚本成功且响应丢失仍有网络歧义，不能声称任何故障都可无损重试；当前配置针对单 Redis，不包含 Redis Cluster 的跨槽支持。
- GitHub Actions 文件已写入工作区，远端 CI 尚未执行（此工作目录没有 Git 元数据，未推送）。
- 不启动生产环境，不修改用户既有 MySQL 数据，不停止用户现有服务。已运行的后端要按正常流程重启后才加载代码。

运行：安装前端依赖并准备 Chromium（`npm ci`、`npx playwright install chromium`）；在 hotel-back 执行 `mvnw.cmd test`，设置 `RUN_BROWSER_TESTS=true` 可同时运行全部浏览器场景。本机可设置 `E2E_CHROME_PATH` 指向已有 Chrome。Node 22、JDK 17 与 CI 一致。
