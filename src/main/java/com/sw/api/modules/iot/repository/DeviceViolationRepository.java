package com.sw.api.modules.iot.repository;

import com.sw.api.modules.iot.model.DeviceViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceViolationRepository extends JpaRepository<DeviceViolation, UUID> {
    List<DeviceViolation> findAllBySmartPlug_IdOrderByDetectedAtDesc(UUID smartPlugId);
}
