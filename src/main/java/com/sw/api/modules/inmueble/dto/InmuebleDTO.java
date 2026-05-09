package com.sw.api.modules.inmueble.dto;

import java.util.UUID;

public record InmuebleDTO(
    UUID id,
    String tipoInmueble,
    java.math.BigDecimal areaTerreno,
    java.math.BigDecimal areaConstruida,
    Integer habitaciones,
    Integer banos,
    Integer garajes,
    Integer antiguedadAnios,
    UbicacionDTO ubicacion
) {}
