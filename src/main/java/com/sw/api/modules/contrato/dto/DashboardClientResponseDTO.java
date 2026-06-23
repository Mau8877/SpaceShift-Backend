package com.sw.api.modules.contrato.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardClientResponseDTO {
    private String id;
    private String nombre;
    private String correo;
    private String telefono;
    private String tipoCliente; // INQUILINO, COMPRADOR, HUESPED, ANTICRESISTA
    private String estado;      // ACTIVO, HISTORICO, PENDIENTE
    private String inmueble;
    private String contrato;
    private String tipoContrato;
    private String fechaInicio;
    private String fechaFin;
    private String ultimaActividad;
    private String moneda;
    private BigDecimal montoContrato;
    private boolean contratoPorVencer;
}
