package com.orderservice.service;

import com.orderservice.client.UserClient;
import com.orderservice.dto.UserResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserIntegrationServiceImplTest {

    @Mock
    private UserClient userClient;

    @InjectMocks
    private UserIntegrationServiceImpl userIntegrationService;

    private UserResponseDto baseUser;
    private UserResponseDto fullUser;

    @BeforeEach
    void setUp() {
        baseUser = new UserResponseDto();
        baseUser.setId(1L);
        baseUser.setEmail("test@example.com");

        fullUser = new UserResponseDto();
        fullUser.setId(1L);
        fullUser.setName("Ivan");
        fullUser.setSurname("Ivanov");
        fullUser.setEmail("test@example.com");
    }

    @Test
    void fetchUserByEmail_Success() {
        Long userId = 1L;
        String email = "test@example.com";

        when(userClient.getUserById(userId)).thenReturn(baseUser);
        when(userClient.getUserByEmail(email)).thenReturn(fullUser);

        UserResponseDto result = userIntegrationService.fetchUserByEmail(userId);

        assertNotNull(result);
        assertEquals("Ivan", result.getName());
        assertEquals(email, result.getEmail());

        verify(userClient).getUserById(userId);
        verify(userClient).getUserByEmail(email);
    }

    @Test
    void userFallback_ShouldReturnPlaceholderDto() {
        Long userId = 1L;
        Throwable exception = new RuntimeException("Connection failed");

        UserResponseDto result = userIntegrationService.userFallback(userId, exception);
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("Service Unavailable", result.getName());
        assertEquals("N/A", result.getSurname());
        assertEquals("N/A", result.getEmail());
    }
}