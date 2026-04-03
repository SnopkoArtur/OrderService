package com.orderservice.service;

import com.orderservice.dao.ItemRepository;
import com.orderservice.dao.OrderRepository;
import com.orderservice.dto.*;
import com.orderservice.entity.*;
import com.orderservice.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private UserIntegrationService userIntegrationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrder_Success() {
        Long userId = 1L;
        Item item = new Item();
        item.setId(10L);
        item.setPrice(new BigDecimal("100.00"));
        item.setName("Test Item");

        OrderRequestDto request = new OrderRequestDto();
        OrderItemRequestDto itemReq = new OrderItemRequestDto();
        itemReq.setItemId(10L);
        itemReq.setQuantity(2);
        request.setItems(List.of(itemReq));

        Order order = new Order();
        order.setId(1L);

        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(orderRepository.save(any())).thenReturn(order);

        OrderResponseDto orderResponseDto = new OrderResponseDto();
        orderResponseDto.setTotalPrice(new BigDecimal("200.00"));
        when(orderMapper.toDto(any())).thenReturn(orderResponseDto);
        when(userIntegrationService.fetchUserById(userId)).thenReturn(new UserResponseDto());

        OrderResponseDto result = orderService.createOrder(request, userId);

        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getTotalPrice());
        verify(orderRepository).save(any());
    }
}