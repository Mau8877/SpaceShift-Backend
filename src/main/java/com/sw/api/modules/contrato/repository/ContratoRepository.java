package com.sw.api.modules.contrato.repository;

import com.sw.api.modules.contrato.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, UUID> {

    List<Contrato> findByPropietarioIdOrderByCreatedDateDesc(UUID propietarioId);
    List<Contrato> findByClienteIdOrderByCreatedDateDesc(UUID clienteId);

    @Query("SELECT COUNT(c) > 0 FROM Contrato c WHERE c.inmueble.id = :inmuebleId " +
           "AND c.estadoContrato = 'VIGENTE' " +
           "AND (:fechaInicio < c.fechaFin AND :fechaFin > c.fechaInicio)")
    boolean overlapsWithExistingBooking(@Param("inmuebleId") UUID inmuebleId, 
                                        @Param("fechaInicio") LocalDate fechaInicio, 
                                        @Param("fechaFin") LocalDate fechaFin);
}
