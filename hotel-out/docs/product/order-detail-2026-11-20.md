# 订单详情卡改造记录

## 需求

在“我的订单”中展示已预订酒店的完整信息，参考移动端订单详情布局，同时保留原有订单筛选、取消、退款和再次预订能力。

## 实现

- 新增 `src/components/business/OrderDetailCard.vue`，统一展示酒店封面、酒店名称与地址、地图入口、联系商家、房源入口、房型属性、入住/离店时间、入住人、下单时间、订单号和金额；联系商家进入现有私信页面。
- 新增 `src/utils/orderDisplay.js`，将真实接口字段、历史 Mock 字段和酒店/房型信息归一为订单展示模型；旧中文状态会反向转换为后端状态码，筛选条件保持可用。
- Mock 订单列表按 `hotelId` 关联演示酒店，自动补充封面、地址和第一间房型信息；真实接口由后端直接返回同类字段。
- `hotel-back` 的 `OrderView` 增加酒店封面、地址、房型名称、房型图片、面积、床型、最大入住人数和设施字段，原有字段保持兼容。
- 订单详情卡使用响应式栅格，桌面端分栏展示，窄屏端自动堆叠并隐藏敏感手机号中间四位。

## 验证证据

- 前端 `npm test`：35 个测试全部通过。
- 前端 `npm run lint`：通过。
- 前端 `npm run build`：通过。
- 后端 `mvnw test -q`：通过（使用 `E:\\mysql\\jdk` 的 Java 17）。
- 后端 `mvnw package -q -DskipTests`：通过。
- Mock 浏览器验收截图：`hotel-out/test-artifacts/order-detail/desktop.png`、`hotel-out/test-artifacts/order-detail/mobile.png`。桌面端确认订单卡、图片、房型和时间轴可见；移动端确认无横向溢出。

## 未验证项

- 未在当前环境连接真实 MySQL 数据库和已部署后端接口做数据联调；后端 DTO 与前端兼容逻辑已完成编译验证。
- 地图按钮打开高德搜索链接，实际导航效果取决于用户浏览器和高德页面可用性。
