package com.school.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.hotel.entity.Order;
import com.school.hotel.mapper.OrderMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Lazy(false)
public class OrderTimeoutScheduler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderTimeoutScheduler.class);
    private static final int BATCH_SIZE=200;
    private final ObjectProvider<OrderMapper> orderMapperProvider;
    private final ObjectProvider<OrderService> orderServiceProvider;

    public OrderTimeoutScheduler(
            ObjectProvider<OrderMapper> orderMapperProvider,
            ObjectProvider<OrderService> orderServiceProvider
    ) {
        this.orderMapperProvider = orderMapperProvider;
        this.orderServiceProvider = orderServiceProvider;
    }

    @Scheduled(
            initialDelayString = "${hotel.order.payment-timeout-initial-delay-ms:60000}",
            fixedDelayString = "${hotel.order.payment-timeout-scan-ms:60000}"
    )
    public void cancelExpiredOrders() {
        OrderMapper orderMapper = orderMapperProvider.getObject();
        OrderService orderService = orderServiceProvider.getObject();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        long lastId = 0;
        while (true) {
            List<Order> expired = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderStatus, "PENDING_PAYMENT")
                    .lt(Order::getCreateTime, cutoff).gt(Order::getId, lastId)
                    .orderByAsc(Order::getId).last("LIMIT " + BATCH_SIZE));
            if (expired.isEmpty()) return;
            for (Order order : expired) {
                try { orderService.cancelExpired(order); }
                catch (RuntimeException failure) {
                    log.warn("Could not expire order {} ({}); continuing batch", order.getId(), failure.getClass().getSimpleName());
                }
            }
            lastId = expired.get(expired.size()-1).getId();
            if (expired.size() < BATCH_SIZE) return;
        }
    }
}
