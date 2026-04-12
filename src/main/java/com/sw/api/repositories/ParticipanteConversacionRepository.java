package com.sw.api.repositories;

import com.sw.api.models.Chat.ParticipanteConversacion;
import com.sw.api.models.Chat.ParticipanteConversacionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ParticipanteConversacionRepository extends JpaRepository<ParticipanteConversacion, ParticipanteConversacionId> {

    @Query("SELECT pc FROM ParticipanteConversacion pc JOIN FETCH pc.conversacion c WHERE pc.usuario.id = :usuarioId ORDER BY c.actualizadoEn DESC")
    List<ParticipanteConversacion> findAllConversacionesByUsuarioIdOrderByActualizadoEnDesc(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT pc FROM ParticipanteConversacion pc WHERE pc.conversacion.id = :conversacionId AND pc.usuario.id != :usuarioId")
    List<ParticipanteConversacion> findOtrosParticipantes(@Param("conversacionId") UUID conversacionId, @Param("usuarioId") UUID usuarioId);
}
