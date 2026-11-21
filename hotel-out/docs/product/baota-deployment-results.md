# 宝塔前端部署准备与验证报告

日期：2026-11-18。范围：域名根目录静态前端 + 同机 8081 后端反代。计划见 [baota-deployment-plan.md](./baota-deployment-plan.md)。

## 问题、影响与最终处理

| Bug                                                                 | 原因                                                                                                                                           | 解决方式                                                                                                         |
| ------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| 按常见 Nginx 回退写法部署后，直接打开景点页 `/attractions` 返回 403 | `public/attractions` 静态图片目录与 Vue 页面同名；`try_files $uri $uri/ /index.html` 优先匹配目录，重定向到 `/attractions/` 后无法找到目录首页 | 提供的宝塔配置改为 `try_files $uri /index.html`，仅匹配实际文件；景点页面交给 Vue，目录内 JPG 仍作为静态资源返回 |

复现：生产构建后用 Nginx 指向 dist，设置包含 `$uri/` 的回退；访问 `/attractions`，被转向 `/attractions/`，实际 HTTP 403。隔离验证脚本也在该路由断言失败，见 `test-artifacts/baota/attractions-before.log`。修改后，同脚本对 `/attractions`、`/attractions/`、`/attractions/1` 及真实 JPG 文件均通过，Chrome 直接打开并刷新景点页正常。

## 部署整理

- `vite.config.js`：显式指定 `base: '/'`，与已确定的域名根目录部署一致。保留开发服务器配置和 Vue History 路由。
- `.env.production`：保持真实接口 `VITE_USE_MOCK=false`、同源 `/api`；移除仅供开发代理使用的 `VITE_BACKEND_URL`，以注释明确生产后端地址在 Nginx 中配置。
- `deploy/baota-locations.conf`：提供路由回退、入口 no-cache、哈希资源长期缓存及缺失资源 404、API/上传代理。使用 `^~` 避免图片正则截获上传请求；保留 `/api` 前缀和查询参数；总请求上限 50MB，API 读取超时 60 秒。代理语义参考 [Nginx 官方 proxy_pass 文档](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass)。
- `README.md`：补充打包、上传位置、宝塔现有规则合并、生产模式、真实接口验收及更新/回滚步骤。
- `.gitignore`：忽略生成的 release 目录。
- `release/hotel-frontend-baota.zip`：根层直接包含 index.html，内部 ZIP 路径统一 `/`。包含 114 个静态文件，5,653,094 字节（约 5.39 MiB）；不含源码、数据库配置或部署文档。

ZIP SHA-256：`2dd61b88bc97340c13c0a8931454d69917c86fa1709a7f73e1ea71601fb946b8`。

## 实际验证与证据

本机构建使用 Node.js v24.22.0、现有安装依赖；本次没有新增依赖，没有重新执行 npm ci。Nginx 使用从官方获取的 Windows 1.30.6，仅监听本机；加载交付的同一份 locations 配置，测试包装层补充本机端口、dist 根目录和图片正则冲突样例。请求由隔离 HTTP 应答服务接收，不接触业务数据库，也不保存凭证。

| 验证                     | 实际结果                                                                                                                                                                                   | 证据                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------- |
| `npm test`               | 25/25 通过，退出码 0                                                                                                                                                                       | `test-artifacts/baota/unit.log`                                   |
| `npm run lint`           | 通过，退出码 0                                                                                                                                                                             | `test-artifacts/baota/lint.log`                                   |
| `npm run build`          | 生产构建通过，退出码 0                                                                                                                                                                     | `test-artifacts/baota/build.log`                                  |
| `nginx -t`               | 配置语法与加载成功                                                                                                                                                                         | `test-artifacts/baota/nginx-config.log`                           |
| 生产静态文件、反代与 ZIP | 17 项通过：7 个直达路由、入口缓存、哈希资源缓存、缺失 JS 404、API 前缀/参数、2MiB POST 请求体转发、503 JSON 保留、上传图片优先级、景点 JPG、public 文件一致性、ZIP CRC/根目录/逐文件一致性 | `test-artifacts/baota/transport-and-package.json`                 |
| Chrome 生产产物          | 首页、搜索直达/刷新、390px 登录页、1280px 景点直达/刷新均渲染；所检页面无横向溢出、损坏图片及 Console error/warn；首页真实模式发出同源 API 请求（测试应答为空数据）                        | `test-artifacts/baota/browser-results.json`、`fixture-paths.json` |

截图：`test-artifacts/baota/home-production.jpg`、`login-production-mobile.jpg`、`attractions-production.jpg`。截图与空数据应答仅证明静态部署及前端渲染，不代表酒店业务数据或登录功能通过。

临时传输验证脚本位于 `D:/overword/.codex-artifacts/baota-nginx/verify.py`；它绑定本机 8081/23880，不用于部署，不应在已有同端口后端的环境直接运行。本次结束时停止临时服务。

## 待部署、未验证和限制

- 用户提供云端后端地址后，进行了两个公开只读接口检查：`http://103.212.186.243:8081/api/ai/status` 与 `/api/hotels?pageNum=1&pageSize=1` 均 HTTP 200、业务 code 200；AI 当前 disabled，酒店分页返回 total=50、当前页 1 条。证据：`test-artifacts/baota/cloud-public-api.json`。这只证明公开接口可达及酒店列表能够返回，不等于登录、订单或 Redis 验收通过。
- 未登录宝塔，未修改云服务器，未发布前端到公网；云端前端部署待用户上传 ZIP 并合并 Nginx 配置。如果前端和该后端在同一台服务器，交付配置保留 `127.0.0.1:8081`，前端不需要改为公网 API 地址。
- 未验证用户服务器的 Linux/Nginx 版本、现有宝塔配置、域名/HTTPS。需要在宝塔执行配置检查，处理既有 location/client_max_body_size 的重复项。
- 除上述公开接口检查外，未验证真实 Java/MySQL/Redis 的完整链路、登录、订单、AI 模型调用与上传持久化。部署后按 README 使用测试数据验收。
- 后端若不直接运行在同机 8081，必须修改两个 proxy_pass 地址；前端当前按域名根目录部署，不支持原样放入 `/hotel/` 等子目录。
- 没有 Git 元数据，未提交或创建 PR。作者完成配置与产物自查；未开展多代理评审。
