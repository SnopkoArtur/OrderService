package com.orderservice.service;

import com.orderservice.dao.ItemRepository;
import com.orderservice.dao.OrderRepository;
import com.orderservice.dto.*;
import com.orderservice.entity.*;
import com.orderservice.exception.ResourceNotFoundException;
import com.orderservice.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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

    private Order testOrder;
    private OrderResponseDto testOrderDto;
    private UserResponseDto testUserDto;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(100L);
        testOrder.setTotalPrice(new BigDecimal("200.00"));
        testOrder.setStatus("PENDING");

        testOrderDto = new OrderResponseDto();
        testOrderDto.setId(1L);
        testOrderDto.setTotalPrice(new BigDecimal("200.00"));

        testUserDto = new UserResponseDto();
        testUserDto.setId(100L);
        testUserDto.setName("Ivan");
    }

    @Test
    void createOrder_Success() {
        Long userId = 1L;
        Item item = new Item();
        item.setId(10L);
        item.setPrice(new BigDecimal("100.00"));

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
        when(userIntegrationService.fetchUserByEmail(userId)).thenReturn(new UserResponseDto());

        OrderResponseDto result = orderService.createOrder(request, userId);

        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getTotalPrice());
        verify(orderRepository).save(any());
    }

    @Test
    void getOrderById_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.toDto(testOrder)).thenReturn(testOrderDto);
        when(userIntegrationService.fetchUserByEmail(100L)).thenReturn(testUserDto);

        OrderResponseDto result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userIntegrationService).fetchUserByEmail(100L);
    }

    @Test
    void getOrderById_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(1L));
    }

    @Test
    void getOrders_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(orderMapper.toDto(any(Order.class))).thenReturn(testOrderDto);
        when(userIntegrationService.fetchUserByEmail(100L)).thenReturn(testUserDto);

        Page<OrderResponseDto> result = orderService.getOrders(null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void updateOrderStatus_Success() {
        String newStatus = "PAID";
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.toDto(testOrder)).thenReturn(testOrderDto);
        when(userIntegrationService.fetchUserByEmail(100L)).thenReturn(testUserDto);

        OrderResponseDto result = orderService.updateOrderStatus(1L, newStatus);

        assertNotNull(result);
        assertEquals("PAID", testOrder.getStatus());
        verify(orderMapper).toDto(testOrder);
    }

    @Test
    void deleteOrder_Success() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.deleteOrder(1L);

        verify(orderRepository).deleteById(1L);
    }

    @Test
    void deleteOrder_NotFound() {
        when(orderRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> orderService.deleteOrder(1L));
        verify(orderRepository, never()).deleteById(anyLong());
    }

    @Test
    void getOrdersByUserId_Success() {
        Long userId = 1L;
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(testOrder));
        when(orderMapper.toDto(testOrder)).thenReturn(testOrderDto);
        when(userIntegrationService.fetchUserByEmail(userId)).thenReturn(testUserDto);

        List<OrderResponseDto> result = orderService.getOrdersByUserId(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository).findAllByUserId(userId);
    }
}
