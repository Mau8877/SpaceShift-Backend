package com.sw.api.modules.contrato.dto;

import com.sw.api.modules.contrato.model.EstadoContrato;
import com.sw.api.modules.contrato.model.TipoContrato;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class ContratoResponseDTO {
    private UUID id;
    private String codigo;
    private UUID idInmueble;
    private String inmuebleTitulo;
    private UUID idPublicacion;
    private UUID idPropietario;
    private String propietarioNombre;
    private UUID idCliente;
    private String clienteNombre;
    private TipoContrato tipoContrato;
    private EstadoContrato estadoContrato;
    private BigDecimal montoAcordado;
    private BigDecimal monto;
    private String moneda;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer cantidadHuespedes;
    private Integer noches;
    private String documentoUrl;
    private String observacion;
    private Map<String, Object> especificaciones;
    private java.util.List<java.util.Map<String, Object>> dispositivosInmueble;
    private String condicionesInmueble;
    private String multasSancionesInmueble;
    private LocalDateTime createdDate;
    private LocalDateTime createdAt;
    private String transactionHash;
}
