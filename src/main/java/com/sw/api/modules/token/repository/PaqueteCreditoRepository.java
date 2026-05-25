package com.sw.api.modules.token.repository;

import com.sw.api.modules.token.model.PaqueteCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PaqueteCreditoRepository extends JpaRepository<PaqueteCredito, UUID> {
}
