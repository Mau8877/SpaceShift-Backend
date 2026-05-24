package com.sw.api.modules.token.repository;

import com.sw.api.modules.token.model.TransaccionCredito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TransaccionCreditoRepository extends JpaRepository<TransaccionCredito, UUID> {
    Page<TransaccionCredito> findByUsuarioIdOrderByCreatedDateDesc(UUID usuarioId, Pageable pageable);
}
