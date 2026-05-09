package com.sw.api.modules.inmueble.repository;

import com.sw.api.modules.inmueble.model.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UbicacionRepository extends JpaRepository<Ubicacion, UUID> {
}
