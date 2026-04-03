package com.orderservice.service;

import com.orderservice.client.UserClient;
import com.orderservice.dao.ItemRepository;
import com.orderservice.dao.OrderRepository;
import com.orderservice.dto.OrderResponseDto;
import com.orderservice.dto.OrderRequestDto;
import com.orderservice.entity.Order;
import com.orderservice.entity.OrderItem;
import com.orderservice.exception.ResourceNotFoundException;
import com.orderservice.mapper.OrderMapper;
import com.orderservice.specification.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.orderservice.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserClient userClient;
    private final UserIntegrationService userIntegrationService;

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto, Long currentUserId) {
        Order order = new Order();
        order.setUserId(currentUserId);
        order.setStatus("PENDING");
        order.setDeleted(false);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (var itemReq : requestDto.getItems()) {
            Item item = itemRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemReq.getItemId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(item);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItems.add(orderItem);

            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(lineTotal);
        }

        order.setOrderItems(orderItems);
        order.setTotalPrice(total);

        Order savedOrder = orderRepository.save(order);
        return enrichWithUserInfo(orderMapper.toDto(savedOrder), currentUserId);
    }

    @Override
    public OrderResponseDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        return enrichWithUserInfo(orderMapper.toDto(order), order.getUserId());
    }

    @Override
    public Page<OrderResponseDto> getOrders(LocalDateTime from, LocalDateTime to, List<String> statuses, Pageable pageable) {
        return orderRepository.findAll(OrderSpecifications.getFilter(from, to, statuses), pageable)
                .map(order -> enrichWithUserInfo(orderMapper.toDto(order), order.getUserId()));
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(status);
        return enrichWithUserInfo(orderMapper.toDto(order), order.getUserId());
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found: " + id);
        }
        orderRepository.softDelete(id);
    }

    @Override
    public List<OrderResponseDto> getOrdersByUserId(Long userId) {
        return orderRepository.findAllByUserIdAndDeletedFalse(userId).stream()
                .map(order -> enrichWithUserInfo(orderMapper.toDto(order), userId))
                .toList();
    }

    private OrderResponseDto enrichWithUserInfo(OrderResponseDto dto, Long userId) {
        dto.setUser(userIntegrationService.fetchUserById(userId));
        return dto;
    }
}