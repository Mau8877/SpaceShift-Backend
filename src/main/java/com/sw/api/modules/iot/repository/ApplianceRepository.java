package com.sw.api.modules.iot.repository;

import com.sw.api.modules.iot.model.Appliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApplianceRepository extends JpaRepository<Appliance, UUID> {
}
