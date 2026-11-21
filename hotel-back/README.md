# Hotel Back

民宿/酒店预订系统后端，使用 Java 17、Spring Boot 3、MyBatis-Plus、MySQL 8、Spring Security/JWT 和 Redis。

## 初始化数据库

在 MySQL 8 执行 `sql/hotel_booking.sql`。脚本创建 `hotel_booking`、业务表、索引、样例房源和演示账号。

| 角色 | 手机号 | 密码 |
| --- | --- | --- |
| 管理员 | `13800000001` | `Hotel@123` |
| 商家 | `13800000002` | `Hotel@123` |
| 住客 | `13800000003` | `Hotel@123` |

数据库中仅保存 BCrypt 哈希。

## 开发环境

默认 MySQL 地址为 `jdbc:mysql://127.0.0.1:3306/hotel_booking`，用户名和密码均为 `root`。可以通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`HOTEL_UPLOAD_PATH` 覆盖。

开发环境 Redis 可选；不可用时刷新令牌和短期数据使用进程内存。生产 profile 要求 Redis。

```powershell
$env:JAVA_HOME='你的 JDK 17 安装目录'
.\mvnw.cmd spring-boot:run
```

启动后端前必须先导入 SQL。若本机 `8080` 已被占用，可临时设置 `$env:SERVER_PORT='8081'`。

接口文档：`http://127.0.0.1:8080/doc.html`

## 前端联调

`hotel-out/.env.development` 默认使用真实 REST API。Vite 将 `/api` 和 `/uploads` 转发到 `http://127.0.0.1:8080`。将 `VITE_USE_MOCK` 改为 `true` 可恢复本地演示数据。

## 构建与测试

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

## IDEA 开发启动优化

IDEA 的 `HotelApplication` 运行配置已加入 `-XX:TieredStopAtLevel=1`。该参数只缩短本地开发冷启动时间，不会进入打包产物或生产环境。订单超时扫描在服务启动 60 秒后首次执行，之后仍每 60 秒执行一次。
