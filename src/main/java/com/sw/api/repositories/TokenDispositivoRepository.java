package com.sw.api.repositories;

import com.sw.api.models.Chat.TokenDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenDispositivoRepository extends JpaRepository<TokenDispositivo, UUID> {
    Optional<TokenDispositivo> findByTokenFcm(String tokenFcm);
}
