package com.orderservice.service;

import com.orderservice.client.UserClient;
import com.orderservice.dao.ItemRepository;
import com.orderservice.dao.OrderRepository;
import com.orderservice.dto.*;
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
    public OrderResponseDto createOrder(OrderRequestCreateDto requestDto) {
        UserResponseDto userFullInfo = userIntegrationService.fetchUserByEmail(requestDto.getUserEmail());

        Order order = new Order();
        order.setUserId(userFullInfo.getId());
        order.setStatus("PENDING");
        order.setDeleted(false);
        order.setUserEmail(requestDto.getUserEmail());

        calculateTotalAndSetItems(requestDto.getItems(), order);

        Order savedOrder = orderRepository.save(order);
        OrderResponseDto response = orderMapper.toDto(savedOrder);
        response.setUser(userFullInfo);
        return response;
    }

    @Override
    public OrderResponseDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        return enrichWithUserInfo(orderMapper.toDto(order), order.getUserEmail());
    }

    @Override
    public Page<OrderResponseDto> getOrders(LocalDateTime from, LocalDateTime to, List<String> statuses, Pageable pageable) {
        return orderRepository.findAll(OrderSpecifications.getFilter(from, to, statuses), pageable)
                .map(order -> enrichWithUserInfo(orderMapper.toDto(order), order.getUserEmail()));
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrder(Long id, OrderRequestUpdateDto orderRequestDto) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.getOrderItems().clear();
        order.setStatus(orderRequestDto.getStatus());
        UserResponseDto userFullInfo = null;

        if (orderRequestDto.getUserEmail() != null && !order.getUserEmail().equals(orderRequestDto.getUserEmail())){
            userFullInfo = userIntegrationService.fetchUserByEmail(orderRequestDto.getUserEmail());
            order.setUserId(userFullInfo.getId());
            order.setUserEmail(orderRequestDto.getUserEmail());
        }

        calculateTotalAndSetItems(orderRequestDto.getItems(), order);
        Order savedOrder = orderRepository.save(order);
        if (userFullInfo != null){
            OrderResponseDto response = orderMapper.toDto(savedOrder);
            response.setUser(userFullInfo);
            return response;
        }
        return enrichWithUserInfo(orderMapper.toDto(savedOrder), order.getUserEmail());
    }

    private void calculateTotalAndSetItems(List<OrderItemRequestDto> items, Order order) {
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = order.getOrderItems();

        for (var itemReq : items) {
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

        order.setTotalPrice(total);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found: " + id);
        }
        orderRepository.deleteById(id);
    }

    @Override
    public List<OrderResponseDto> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findAllByUserId(userId);
        if (orders.isEmpty()) return List.of();

        UserResponseDto userDto = userIntegrationService.fetchUserByEmail(orders.get(0).getUserEmail());

        return orders.stream()
                .map(order -> {
                    OrderResponseDto dto = orderMapper.toDto(order);
                    dto.setUser(userDto);
                    return dto;
                })
                .toList();
    }

    private OrderResponseDto enrichWithUserInfo(OrderResponseDto dto, String email) {
        dto.setUser(userIntegrationService.fetchUserByEmail(email));
        return dto;
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        order.setStatus(status);
        orderRepository.save(order);
    }
}