package com.sw.api.modules.iot.repository;

import com.sw.api.modules.iot.model.PlugAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlugAssignmentRepository extends JpaRepository<PlugAssignment, UUID> {
    Optional<PlugAssignment> findFirstBySmartPlug_IdAndUnassignedAtIsNull(UUID smartPlugId);

    List<PlugAssignment> findAllByUnassignedAtIsNull();
}
