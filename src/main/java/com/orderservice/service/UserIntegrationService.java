package com.orderservice.service;

import com.orderservice.dto.UserResponseDto;

/**
 * Assistance interface for communicating with UserService
 */
public interface UserIntegrationService {
    /**
     * Gets user info from remote service
     *
     * @param id user id using which email and then user data acquired
     * @return user data or filled temporal data
     */
    UserResponseDto fetchUserByEmail(Long id);
}
