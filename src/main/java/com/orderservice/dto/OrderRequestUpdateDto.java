package com.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequestUpdateDto {
    @NotEmpty
    @Valid
    private List<OrderItemRequestDto> items;

    @Email()
    private String userEmail;

    @NotBlank()
    private String status;
}

