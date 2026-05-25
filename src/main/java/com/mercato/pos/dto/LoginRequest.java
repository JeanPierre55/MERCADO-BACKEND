package com.mercato.pos.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Username no debe estar vacío")
    String username,
    
    @NotBlank(message = "Password no debe estar vacío")
    String password
) {}
