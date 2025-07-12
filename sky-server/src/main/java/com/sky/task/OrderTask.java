package com.sky.task;


import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * check expired payment order every minute
     */
    @Scheduled(cron = "0 * * * * ?")
    // @Scheduled(cron = "0/10 * * * * ?") // test functionality
    public void handleExpiredOrders() {
        log.info("handle expired orders, {}", LocalDateTime.now());

        // it's considered expired if it exceeds 15 minutes
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        // get expired pending payment orders from database
        List<Orders> orders = orderMapper.getOrdersByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);

        // update expired pending payment orders
        if (orders != null && !orders.isEmpty()) {
            for (Orders order : orders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("order payment expired");
                orderMapper.update(order);
            }
        }
    }

    /**
     * handle delivery orders at 1am per day
     */
    @Scheduled(cron = "0 0 1 * * ?")
    // @Scheduled(cron = "0/5 * * * * ?") // test functionality
    public void handleDeliveryOrders() {
        log.info("handle delivery orders, {}", LocalDateTime.now());

        // handle former day's orders at 01:00 am every day
        LocalDateTime time = LocalDateTime.now().plusHours(-1);

        // get delivery orders from database
        List<Orders> orders = orderMapper.getOrdersByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);

        // update orders
        if (orders != null && !orders.isEmpty()) {
            for (Orders order : orders) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }

}
