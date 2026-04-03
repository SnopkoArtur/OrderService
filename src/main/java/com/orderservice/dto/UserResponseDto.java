package com.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponseDto implements Serializable {
    private Long id;
    private String name;
    private String surname;
    private String email;
}