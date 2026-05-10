package com.sw.api.modules.chat.repository;

import com.sw.api.modules.chat.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface ConversacionRepository extends JpaRepository<Conversacion, UUID> {

    @Query("SELECT c FROM Conversacion c " +
           "JOIN ParticipanteConversacion pc1 ON c.id = pc1.conversacion.id " +
           "JOIN ParticipanteConversacion pc2 ON c.id = pc2.conversacion.id " +
           "WHERE c.propiedad.id = :inmuebleId " +
           "AND pc1.usuario.id = :clienteId " +
           "AND pc2.usuario.id = :propietarioId")
    Optional<Conversacion> findByInmuebleAndParticipantes(
            @Param("inmuebleId") UUID inmuebleId,
            @Param("clienteId") UUID clienteId,
            @Param("propietarioId") UUID propietarioId
    );
}
