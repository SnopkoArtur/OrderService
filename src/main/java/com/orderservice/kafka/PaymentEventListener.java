package com.orderservice.kafka;

import com.orderservice.dto.PaymentEventDto;
import com.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void handlePaymentEvent(PaymentEventDto event) {
        String newOrderStatus = event.getStatus().equals("SUCCESS") ? "PAID" : "PAYMENT_FAILED";

        orderService.updateOrderStatus(event.getOrderId(), newOrderStatus);
    }
}