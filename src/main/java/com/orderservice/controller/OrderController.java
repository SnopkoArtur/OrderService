package com.orderservice.controller;

import com.orderservice.dto.OrderRequestCreateDto;
import com.orderservice.dto.OrderRequestUpdateDto;
import com.orderservice.dto.OrderResponseDto;
import com.orderservice.service.OrderService;
import com.orderservice.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST-controller for order management
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates new order for current user
     *
     * @param requestDto order info
     * @return created order
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestCreateDto requestDto) {
        return new ResponseEntity<>(orderService.createOrder(requestDto), HttpStatus.CREATED);
    }

    /**
     * Provides info about order with given id
     *
     * @param id order id
     * @return order info.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long id) {
        OrderResponseDto order = orderService.getOrderById(id);

        if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.getCurrentUserId().equals(order.getUser().getId())) {
            throw new AccessDeniedException("Access denied: You don't own this order");
        }

        return ResponseEntity.ok(order);
    }

    /**
     * Provides list of all order with filtration by datetime
     *
     * @param from     starting datetime
     * @param to       ending datetime
     * @param statuses statuses for filtration
     * @param pageable params for pagination
     * @return list of orders
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponseDto>> getAllOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<String> statuses,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrders(from, to, statuses, pageable));
    }

    /**
     * Provides all orders of given user
     *
     * @param userId user id
     * @return order list for user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    /**
     * Updates order with given id
     *
     * @param id     order id
     * @param requestDto new order data
     * @return updated order
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> updateOrder(@PathVariable Long id, @Valid  @RequestBody OrderRequestUpdateDto requestDto) {
        return ResponseEntity.ok(orderService.updateOrder(id, requestDto));
    }

    /**
     * Deletes order (soft delete)
     *
     * @param id order id
     * @return No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}