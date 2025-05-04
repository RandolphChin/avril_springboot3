package com.randy.chin.controller;

import com.randy.chin.service.impl.SegmentIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    @Autowired
    private SegmentIdGenerator idGenerator;

    @GetMapping("/order/create")
    public String createOrder() {
        long orderId = idGenerator.nextId("order");
        return "订单ID: " + orderId;
    }
}
