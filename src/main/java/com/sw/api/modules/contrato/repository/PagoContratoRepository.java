package com.sw.api.modules.contrato.repository;

import com.sw.api.modules.contrato.model.PagoContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagoContratoRepository extends JpaRepository<PagoContrato, UUID> {

    List<PagoContrato> findByContratoIdOrderByFechaVencimientoAsc(UUID contratoId);

    Optional<PagoContrato> findByStripePagoId(String stripePagoId);
}
