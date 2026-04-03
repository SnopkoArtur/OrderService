package com.orderservice.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.orderservice.dao.OrderRepository;
import com.orderservice.dto.UserResponseDto;
import com.orderservice.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;
    private Order savedOrder;

    protected UsernamePasswordAuthenticationToken getAuth(Long userId, String role) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        Order order = new Order();
        order.setUserId(2L);
        order.setStatus("PENDING");
        order.setTotalPrice(new BigDecimal("200.00"));
        order.setDeleted(false);
        order.setOrderItems(new ArrayList<>());

        savedOrder = orderRepository.save(order);
    }

    @Test
    void getOrderById_ShouldEnrichWithUserFromWireMock() throws Exception {
        var adminAuth = getAuth(2L, "ADMIN");
        Long userId = 2L;

        UserResponseDto mockUser = new UserResponseDto();
        mockUser.setId(userId);
        mockUser.setName("WireMocked");
        mockUser.setEmail("mock@test.com");

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/v1/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockUser))));

        mockMvc.perform(get("/api/v1/orders/" + savedOrder.getId()).with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("WireMocked"))
                .andExpect(jsonPath("$.user.email").value("mock@test.com"));
    }

    @Test
    void getOrderById_ShouldTriggerCircuitBreakerFallback_WhenUserServiceDown() throws Exception {
        var adminAuth = getAuth(2L, "ADMIN");
        Long userId = 2L;

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/v1/users/" + userId))
                .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/api/v1/orders/" + savedOrder.getId()).with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Service Unavailable"));
    }
}