package com.sw.api.modules.auth.dto;

public record CambiarPasswordRequest(String correo, String codigo, String nuevaPassword) {}