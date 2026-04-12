package com.sw.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sw.api.models.Chat.Conversacion;

import java.util.UUID;

public interface ConversacionRepository extends JpaRepository<Conversacion, UUID> {
}
