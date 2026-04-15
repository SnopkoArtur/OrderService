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
        String email = "test@example.com";

        when(userClient.getUserByEmail(email)).thenReturn(fullUser);

        UserResponseDto result = userIntegrationService.fetchUserByEmail(email);

        assertNotNull(result);
        assertEquals("Ivan", result.getName());
        assertEquals(email, result.getEmail());

        verify(userClient).getUserByEmail(email);
    }

    @Test
    void userFallback_ShouldReturnPlaceholderDto() {
        Throwable exception = new RuntimeException("Connection failed");

        UserResponseDto result = userIntegrationService.userFallback("someemail@email.test", exception);
        assertNotNull(result);
        assertEquals("Service Unavailable", result.getName());
        assertEquals("N/A", result.getSurname());
        assertEquals("N/A", result.getEmail());
    }
}