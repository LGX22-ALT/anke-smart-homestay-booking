# 宝塔域名无法访问排查

日期：2026-11-18。状态：原因已确认，云端修改待用户执行；未修复，未验证最终上线。

## 问题与影响

访问先前对话给出的 `blog.lianguoxi.icu` 无法打开。重新核对用户阿里云 DNS 截图发现实际域名为 `liangguoxi.icu`，此前助手把拼写少写了一个 g，导致宝塔绑定了不存在的域名。无需重新打包或迁移前端目录。

## 复现与实际结果

1. 公共 DNS 查询：`blog.lianguoxi.icu` 返回 NXDOMAIN；`blog.liangguoxi.icu` 返回 A 记录 `103.212.186.243`，TTL 600。
2. 绕过本地代理，直接向服务器 80 端口发送 HTTP 请求：Host 为正确域名时，HTTP 200 但正文是宝塔“没有找到站点”；Host 为错误域名时返回本项目 Vue 入口。说明前端文件已部署，但站点域名绑定不匹配。
3. 两种 Host 的 `/api/ai/status` 都返回 HTTP 404，而公网后端 8081 的该接口此前实测 HTTP/业务 200。前端站点的 API 转发也需要核对配置；404 原因尚不能仅凭外部请求唯一确定。

## 待执行解决方式

- 宝塔 → 网站 → 对应前端网站 → 设置 → 域名管理，添加 `blog.liangguoxi.icu`，端口 80；将错误域名替换或移除。检查生效的 server_name 包含正确域名。
- 网站目录保持 `/www/hotel/hotel-ui`，保留既有前端文件。
- 在该站点 server 块合并 `/api/` 与 `/uploads/` 到同机 `127.0.0.1:8081` 的代理，保留 `/api` 前缀；已有规则应修改，不重复添加。配置检查成功后重新加载 Nginx。
- 验收正确域名的首页与 `/api/ai/status`，后者应为 JSON，HTTP 200/业务 code 200。再验证酒店列表及登录订单；后两项本次没有执行。

## 改动位置与证据

云端拟改位置为宝塔前端站点的域名管理与 Nginx server 配置，当前没有远程修改。证据：`hotel-out/test-artifacts/baota/domain-diagnosis.json`，只保存 DNS 结果及脱敏的响应元数据，不含密码、Cookie 或令牌。

## 用户修改后的复查

正确域名的系统 DNS 和公共 DNS 均已指向 `103.212.186.243`；直接 HTTP 访问首页返回项目 Vue 入口，HTTP 200。主入口 JS 与 router JS 均为 HTTP 200、application/javascript。错误拼写 Host 已不匹配站点，表明域名绑定问题已由用户修正。

仍待处理：`/search`、`/api/ai/status`、`/api/hotels?pageNum=1&pageSize=1` 返回 Nginx 404，后端直接访问 8081 的 AI 状态接口 HTTP/业务 200。可确认站点的 History 回退及 API 反代没有生效，但尚未取得当前 Nginx 配置，无法判断是缺少规则、合并位置错误还是规则被覆盖。需核对该站点的有效 server 配置后在云端修正，再验证。

复查证据：`test-artifacts/baota/domain-recheck.json`（DNS、首页、后端状态等）；两个 JS 请求 HTTP 200 和 search/API 404 在当前会话终端中实测。Chrome 工具打开域名受到工具侧 `ERR_BLOCKED_BY_CLIENT` 限制，没有据此判断用户浏览器状态，也未声称云端页面渲染通过。若用户仍无法加载首页，需提供浏览器实际错误及地址栏协议，继续区分客户端 DNS、代理或 HTTPS 问题。

## 完整替换配置交付

用户提供生效站点配置后，确认其中没有 Vue 回退及 API/上传代理，仅有静态正则缓存与宝塔默认 include。已生成 `deploy/baota-site.conf`，保留原域名、80 端口、`/www/hotel/hotel-ui`、证书验证/扩展 include、安全规则和日志路径；移除纯静态站点不需要的 PHP、外部伪静态 include 与通用 JS/图片缓存规则，统一使用明确的 Vue 回退及代理。

本机对完整 server 块进行了 Nginx 语法与 10 项 HTTP 检查，全部通过，覆盖首页、search/attractions、两个 API、上传图片、缺失资源与敏感路径。为在 Windows 本机验证，只在测试副本中替换 Linux 磁盘/日志路径与监听端口，并省略无法访问的云端宝塔 include；API 使用隔离传输 fixture。交付文件仍保留用户真实 Linux 路径。证据：`test-artifacts/baota/full-site-config-check.json`。

云端仍待用户在宝塔粘贴完整配置、保存检查并重载；外部 include 的实际内容和云端最终访问效果尚未验证，不标记为已修复。
