package com.sw.api.modules.inmueble.dto;

import java.math.BigDecimal;

public record InmuebleRequestDTO(
    String tipoInmueble,
    BigDecimal areaTerreno,
    BigDecimal areaConstruida,
    Integer habitaciones,
    Integer banos,
    Integer garajes,
    Integer antiguedadAnios,
    UbicacionDTO ubicacion,
    java.util.List<java.util.Map<String, Object>> dispositivos,
    String condiciones,
    String multasSanciones
) {}
