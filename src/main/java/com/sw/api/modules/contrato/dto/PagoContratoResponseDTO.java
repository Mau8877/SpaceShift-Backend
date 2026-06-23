package com.sw.api.modules.contrato.dto;

import com.sw.api.modules.contrato.model.EstadoPago;
import com.sw.api.modules.contrato.model.MetodoPago;
import com.sw.api.modules.contrato.model.TipoPago;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PagoContratoResponseDTO {
    private UUID id;
    private UUID idContrato;
    private BigDecimal monto;
    private String moneda;
    private TipoPago tipoPago;
    private EstadoPago estadoPago;
    private MetodoPago metodoPago;
    private LocalDate fechaVencimiento;
    private LocalDateTime fechaPago;
    private String documentoComprobanteUrl;
    private String stripePagoId;
}
