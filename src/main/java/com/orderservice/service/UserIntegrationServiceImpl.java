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
    public UserResponseDto fetchUserByEmail(String email) {
        return userClient.getUserByEmail(email);
    }

    public UserResponseDto userFallback(String email, Throwable t) {
        log.error("Circuit Breaker triggered for User ID {}! Reason: {}", email, t.getMessage());
        UserResponseDto fallback = new UserResponseDto();
        fallback.setId(0L);
        fallback.setName("Service Unavailable");
        fallback.setSurname("N/A");
        fallback.setEmail("N/A");
        return fallback;
    }
}