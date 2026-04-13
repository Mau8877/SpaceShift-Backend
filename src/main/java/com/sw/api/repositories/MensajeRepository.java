package com.sw.api.repositories;

import com.sw.api.models.Chat.Mensaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {
    
    Page<Mensaje> findByConversacionIdOrderByCreadoEnDesc(UUID conversacionId, Pageable pageable);
}
