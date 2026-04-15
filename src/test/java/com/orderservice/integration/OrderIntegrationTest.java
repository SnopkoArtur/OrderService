package com.orderservice.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.orderservice.dao.OrderRepository;
import com.orderservice.dto.OrderItemRequestDto;
import com.orderservice.dto.OrderRequestCreateDto;
import com.orderservice.dto.OrderRequestUpdateDto;
import com.orderservice.dto.UserResponseDto;
import com.orderservice.entity.Item;
import com.orderservice.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
    @Autowired
    private com.orderservice.dao.ItemRepository itemRepository;

    private static final long USER_ID = 2;
    private static final String EMAIL = "mock@test.com";

    protected UsernamePasswordAuthenticationToken getAuth(Long userId, String role) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        wireMockServer.resetAll();

        Order order = new Order();
        order.setUserId(USER_ID);
        order.setStatus("PENDING");
        order.setTotalPrice(new BigDecimal("200.00"));
        order.setDeleted(false);
        order.setOrderItems(new ArrayList<>());
        order.setUserEmail(EMAIL);

        savedOrder = orderRepository.save(order);
    }

    private void stubUserServiceSuccess(Long userId, String email) throws Exception {
        UserResponseDto fullInfo = new UserResponseDto();
        fullInfo.setId(userId);
        fullInfo.setName("WireMocked");
        fullInfo.setEmail(email);
        String encodedEmail = email.replace("@", "%40");

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/v1/users/email/" + encodedEmail))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(fullInfo))));
    }

    @Test
    void getOrderById_ShouldEnrichWithUserFromWireMock() throws Exception {
        var adminAuth = getAuth(2L, "ADMIN");
        Long userId = USER_ID;
        stubUserServiceSuccess(userId, EMAIL);
        mockMvc.perform(get("/api/v1/orders/" + savedOrder.getId()).with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("WireMocked"))
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    void getOrderById_ShouldTriggerCircuitBreakerFallback_WhenUserServiceDown() throws Exception {
        var adminAuth = getAuth(2L, "ADMIN");

        String encodedEmail = EMAIL.replace("@", "%40");

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/v1/users/email/" + encodedEmail))
                .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/api/v1/orders/" + savedOrder.getId()).with(authentication(adminAuth)))
                .andExpect(jsonPath("$.user.name").value("Service Unavailable"));
    }

    @Test
    void createOrder_InvalidArguments_400() throws Exception {
        var adminAuth = getAuth(2L, "ADMIN");

        OrderRequestCreateDto invalidDto = new OrderRequestCreateDto();
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
    void getOrderById_WhenUserNoToken_403() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + savedOrder.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrderById_WhenUserIsNotOwner_403() throws Exception {
        var userAuth = getAuth(12345L, "USER");
        mockMvc.perform(get("/api/v1/orders/" + savedOrder.getId())
                        .with(authentication(userAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrders_WithFiltersAndPagination_ShouldReturnCorrectPage() throws Exception {
        var adminAuth = getAuth(1L, "ADMIN");

        Long userId = USER_ID;
        stubUserServiceSuccess(userId, EMAIL);

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

    @Test
    void updateOrder_Admin_ShouldSucceed() throws Exception {
        var adminAuth = getAuth(1L, "ADMIN");

        Item testItem = itemRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Seed data not found"));

        stubUserServiceSuccess(USER_ID, "EMAIL");

        OrderRequestUpdateDto updateDto = new OrderRequestUpdateDto();
        updateDto.setStatus("COMPLETED");

        OrderItemRequestDto itemReq = new OrderItemRequestDto();
        itemReq.setItemId(testItem.getId());
        itemReq.setQuantity(3);
        updateDto.setItems(List.of(itemReq));

        stubUserServiceSuccess(1L, EMAIL);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/orders/" + savedOrder.getId())
                        .with(authentication(adminAuth))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalPrice").value(2999.97))
                .andExpect(jsonPath("$.user.name").value("WireMocked"))
                .andExpect(jsonPath("$.orderItems.length()").value(1));
    }
}