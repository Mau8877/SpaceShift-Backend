package com.sw.api.modules.inmueble.repository;

import com.sw.api.modules.inmueble.model.Inmueble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

@Repository
public interface InmuebleRepository extends JpaRepository<Inmueble, UUID> {
    @Query("SELECT DISTINCT i.tipoInmueble FROM Inmueble i WHERE i.tipoInmueble IS NOT NULL")
    List<String> findDistinctTipoInmueble();
}
