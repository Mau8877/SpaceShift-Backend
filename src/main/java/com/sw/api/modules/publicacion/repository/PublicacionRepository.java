package com.sw.api.modules.publicacion.repository;

import com.sw.api.modules.publicacion.model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PublicacionRepository extends JpaRepository<Publicacion, UUID> {
}
