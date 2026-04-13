package com.orderservice.service;

import com.orderservice.client.UserClient;
import com.orderservice.dto.UserResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserIntegrationServiceImpl implements UserIntegrationService {

    private final UserClient userClient;

    @Override
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "userFallback")
    public UserResponseDto fetchUserByEmail(Long userId) {
        UserResponseDto baseUser = userClient.getUserById(userId);
        String email = baseUser.getEmail();

        return userClient.getUserByEmail(email);
    }

    public UserResponseDto userFallback(Long id, Throwable t) {
        log.error("Circuit Breaker triggered for User ID {}! Reason: {}", id, t.getMessage());
        UserResponseDto fallback = new UserResponseDto();
        fallback.setId(id);
        fallback.setName("Service Unavailable");
        fallback.setSurname("N/A");
        fallback.setEmail("N/A");
        return fallback;
    }
}