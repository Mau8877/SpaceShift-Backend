package com.sw.api.modules.chat.repository;

import com.sw.api.modules.chat.model.EstadoMensaje;
import com.sw.api.modules.chat.model.Mensaje;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {
    Page<Mensaje> findByConversacionIdOrderByCreadoEnDesc(UUID conversacionId, Pageable pageable);

    int countByConversacionIdAndEstadoNotAndRemitenteIdNot(
        UUID conversacionId, EstadoMensaje estado, UUID remitenteId);

    @Modifying
    @Transactional
    @Query("UPDATE Mensaje m SET m.estado = 'LEIDO' WHERE m.conversacion.id = :conversacionId AND m.remitente.id != :usuarioId AND m.estado != 'LEIDO'")
    void marcarComoLeidos(@Param("conversacionId") UUID conversacionId, @Param("usuarioId") UUID usuarioId);
}
