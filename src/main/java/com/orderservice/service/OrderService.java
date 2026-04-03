package com.orderservice.service;

import com.orderservice.dto.OrderRequestDto;
import com.orderservice.dto.OrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for order management
 */
public interface OrderService {

    /**
     * Creates new order
     *
     * @param requestDto    request data, including all ordered items
     * @param currentUserId user id
     * @return created order
     */
    OrderResponseDto createOrder(OrderRequestDto requestDto, Long currentUserId);

    /**
     * Provides full info about order using id
     *
     * @param id order id
     * @return order information
     */
    OrderResponseDto getOrderById(Long id);

    /**
     * Provides page of orders with filtration
     *
     * @param from     starting date
     * @param to       ending date
     * @param statuses statuses for filtration
     * @param pageable params for pagination
     * @return order page
     */
    Page<OrderResponseDto> getOrders(LocalDateTime from, LocalDateTime to, List<String> statuses, Pageable pageable);

    /**
     * Provides all not delete orders for given user id
     *
     * @param userId user id
     * @return all not deleted orders for given user id
     */
    List<OrderResponseDto> getOrdersByUserId(Long userId);

    /**
     * Updates order status
     *
     * @param id     order id
     * @param status new status string
     * @return updates order
     */
    OrderResponseDto updateOrderStatus(Long id, String status);

    /**
     * Performs soft delete
     *
     * @param id order id
     */
    void deleteOrder(Long id);
}