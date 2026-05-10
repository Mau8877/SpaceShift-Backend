package com.sw.api.modules.inmueble.repository;

import com.sw.api.modules.inmueble.model.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

@Repository
public interface UbicacionRepository extends JpaRepository<Ubicacion, UUID> {
    @Query("SELECT DISTINCT u.zonaBarrios FROM Ubicacion u WHERE u.zonaBarrios IS NOT NULL")
    List<String> findDistinctZonaBarrios();
}
