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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

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

    @Test
    void createOrder_InvalidArguments_400() throws Exception {
        var adminAuth = getAuth(2L, "ADMIN");

        com.orderservice.dto.OrderRequestDto invalidDto = new com.orderservice.dto.OrderRequestDto();
        invalidDto.setItems(java.util.Collections.emptyList());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/orders")
                        .with(authentication(adminAuth))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getOrderById_WhenUserIsNotOwner_403() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + savedOrder.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOrderStatus_Admin_ShouldSucceed() throws Exception {
        var adminAuth = getAuth(1L, "ADMIN");
        String newStatus = "PAID";

        UserResponseDto mockUser = new UserResponseDto();
        mockUser.setId(2L);
        mockUser.setName("WireMocked");

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/v1/users/2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockUser))));

        mockMvc.perform(patch("/api/v1/orders/" + savedOrder.getId() + "/status")
                        .param("status", newStatus)
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(newStatus))
                .andExpect(jsonPath("$.user.name").value("WireMocked"));
    }

    @Test
    void getOrders_WithFiltersAndPagination_ShouldReturnCorrectPage() throws Exception {
        var adminAuth = getAuth(1L, "ADMIN");

        UserResponseDto mockUser = new UserResponseDto();
        mockUser.setId(2L);
        mockUser.setName("WireMocked");

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/v1/users/2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockUser))));

        mockMvc.perform(get("/api/v1/orders")
                        .param("statuses", "PENDING")
                        .param("page", "0")
                        .param("size", "10")
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        String now = java.time.LocalDateTime.now().minusHours(1).toString();
        String later = java.time.LocalDateTime.now().plusHours(1).toString();

        mockMvc.perform(get("/api/v1/orders")
                        .param("from", now)
                        .param("to", later)
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/orders")
                        .param("statuses", "some")
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void deleteOrder_ShouldPerformSoftDeleteA() throws Exception {
        var adminAuth = getAuth(2L, "ADMIN");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/orders/" + savedOrder.getId())
                        .with(authentication(adminAuth)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/orders/" + savedOrder.getId())
                        .with(authentication(adminAuth)))
                .andExpect(status().isNotFound());
    }
}