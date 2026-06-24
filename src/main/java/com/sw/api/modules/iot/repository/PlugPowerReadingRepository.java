package com.sw.api.modules.iot.repository;

import com.sw.api.modules.iot.model.PlugPowerReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlugPowerReadingRepository extends JpaRepository<PlugPowerReading, UUID> {
    Optional<PlugPowerReading> findFirstBySmartPlug_IdOrderByRecordedAtDesc(UUID smartPlugId);

    List<PlugPowerReading> findAllBySmartPlug_IdAndRecordedAtAfterOrderByRecordedAtAsc(UUID smartPlugId,
            LocalDateTime desde);
}
