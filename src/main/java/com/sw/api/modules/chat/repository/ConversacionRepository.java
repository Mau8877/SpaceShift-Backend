package com.sw.api.modules.chat.repository;

import com.sw.api.modules.chat.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversacionRepository extends JpaRepository<Conversacion, UUID> {
}
