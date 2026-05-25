package com.mercato.pos.dto;

public record LoginResponse(
    String token,
    String role,
    UserDto user
) {}
