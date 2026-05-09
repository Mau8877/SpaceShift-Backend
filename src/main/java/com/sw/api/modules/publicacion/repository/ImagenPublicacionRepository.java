package com.sw.api.modules.publicacion.repository;

import com.sw.api.modules.publicacion.model.ImagenPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImagenPublicacionRepository extends JpaRepository<ImagenPublicacion, UUID> {
}
