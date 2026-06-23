package com.sw.api.modules.contrato.dto;

import com.sw.api.modules.contrato.model.TipoContrato;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class ContratoRequestDTO {
    private UUID idInmueble;
    private UUID idPublicacion;
    private UUID idCliente;
    private TipoContrato tipoContrato;
    private BigDecimal montoAcordado;
    private String moneda;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer cantidadHuespedes;
    private String observacion;
    private Map<String, Object> especificaciones;
}
