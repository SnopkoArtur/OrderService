package com.orderservice.kafka;

import com.orderservice.dto.PaymentEventDto;
import com.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    @Test
    void handlePaymentEvent_Success() {
        PaymentEventDto event = new PaymentEventDto();
        event.setOrderId(100L);
        event.setStatus("SUCCESS");

        paymentEventListener.handlePaymentEvent(event);

        verify(orderService, times(1)).updateOrderStatus(100L, "PAID");
    }

    @Test
    void handlePaymentEvent_Failed() {
        PaymentEventDto event = new PaymentEventDto();
        event.setOrderId(101L);
        event.setStatus("FAILED");

        paymentEventListener.handlePaymentEvent(event);

        verify(orderService, times(1)).updateOrderStatus(101L, "PAYMENT_FAILED");
    }
}