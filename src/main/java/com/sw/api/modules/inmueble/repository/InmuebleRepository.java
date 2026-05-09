package com.sw.api.modules.inmueble.repository;

import com.sw.api.modules.inmueble.model.Inmueble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InmuebleRepository extends JpaRepository<Inmueble, UUID> {
}
