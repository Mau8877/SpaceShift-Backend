package com.sw.api.modules.iot.repository;

import com.sw.api.modules.iot.model.PlugUsageSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlugUsageSessionRepository extends JpaRepository<PlugUsageSession, UUID> {
    Optional<PlugUsageSession> findFirstBySmartPlug_IdAndEndedAtIsNull(UUID smartPlugId);
}
