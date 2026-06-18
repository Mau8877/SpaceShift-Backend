package com.sw.api.shared.service;

import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3ServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PerfilRepository perfilRepository;

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        s3Service = new S3Service(s3Presigner, perfilRepository);
    }

    @Test
    void testResolverCarpetaUsuario_WithValidProfile() {
        UUID usuarioId = UUID.randomUUID();
        Perfil perfil = new Perfil();
        perfil.setNombre("Juan Carlos");
        perfil.setApellido("Pérez Gómez");

        when(perfilRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        String folder = s3Service.resolverCarpetaUsuario(usuarioId);
        String expectedName = "juan_carlos_perez_gomez";
        assertEquals(usuarioId.toString() + "/" + expectedName, folder);
    }

    @Test
    void testResolverCarpetaUsuario_WithSpecialCharacters() {
        UUID usuarioId = UUID.randomUUID();
        Perfil perfil = new Perfil();
        perfil.setNombre("María-José");
        perfil.setApellido("Nuñez!!");

        when(perfilRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        String folder = s3Service.resolverCarpetaUsuario(usuarioId);
        // "maria-jose" -> "mariajose" after removing [^a-zA-Z0-9_]
        // "nunez!!" -> "nunez"
        // full name: "María-José Nuñez!!" -> sanitized to "mariajose_nunez"
        String expectedName = "mariajose_nunez";
        assertEquals(usuarioId.toString() + "/" + expectedName, folder);
    }

    @Test
    void testResolverCarpetaUsuario_WithNullApellido() {
        UUID usuarioId = UUID.randomUUID();
        Perfil perfil = new Perfil();
        perfil.setNombre("Ana");
        perfil.setApellido(null);

        when(perfilRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        String folder = s3Service.resolverCarpetaUsuario(usuarioId);
        String expectedName = "ana";
        assertEquals(usuarioId.toString() + "/" + expectedName, folder);
    }

    @Test
    void testResolverCarpetaUsuario_ProfileNotFound() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> s3Service.resolverCarpetaUsuario(usuarioId));
    }
}
