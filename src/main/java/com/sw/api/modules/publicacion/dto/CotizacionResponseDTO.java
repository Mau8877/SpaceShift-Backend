package com.sw.api.modules.publicacion.dto;

import com.sw.api.modules.publicacion.model.Formato3D;

public record CotizacionResponseDTO(
    Integer duracionSegundos,
    int factorPorSegundo,
    int costoCreditos,
    int saldoActual,
    boolean saldoSuficiente,
    Formato3D formato
) {}
