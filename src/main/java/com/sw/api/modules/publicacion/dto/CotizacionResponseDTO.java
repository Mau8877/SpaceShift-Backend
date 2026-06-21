package com.sw.api.modules.publicacion.dto;

public record CotizacionResponseDTO(
    Integer duracionSegundos,
    int factorPorSegundo,
    int costoCreditos,
    int saldoActual,
    boolean saldoSuficiente
) {}
