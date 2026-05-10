package com.sw.api.modules.chat.repository;

import com.sw.api.modules.chat.model.TokenDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenDispositivoRepository extends JpaRepository<TokenDispositivo, UUID> {
    Optional<TokenDispositivo> findByTokenFcm(String tokenFcm);
    List<TokenDispositivo> findByUsuarioId(UUID usuarioId);
}
