package com.sw.api.modules.iot.repository;

import com.sw.api.modules.iot.model.SmartPlug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmartPlugRepository extends JpaRepository<SmartPlug, UUID> {
    Optional<SmartPlug> findByTuyaDeviceId(String tuyaDeviceId);

    List<SmartPlug> findAllByTuyaDeviceIdIn(List<String> tuyaDeviceIds);
}
