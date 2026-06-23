package com.sw.api.modules.contrato.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class FirmaContratoRequestDTO {
    private List<Map<String, Object>> dispositivosAlquilados;
    private BigDecimal montoAcordado;
}
