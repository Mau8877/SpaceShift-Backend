package com.sw.api.modules.publicacion.controller;

import com.sw.api.modules.publicacion.dto.PublicacionRequestDTO;
import com.sw.api.modules.publicacion.dto.PublicacionResponseDTO;
import com.sw.api.modules.publicacion.service.PublicacionService;
import com.sw.api.modules.usuario.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {

    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    @GetMapping("/tipos-transaccion")
    public ResponseEntity<List<String>> obtenerTiposTransaccion() {
        return ResponseEntity.ok(publicacionService.obtenerTiposTransaccionUnicos());
    }

    @PostMapping
    public ResponseEntity<PublicacionResponseDTO> crear(@RequestBody PublicacionRequestDTO dto) {
        PublicacionResponseDTO response = publicacionService.crear(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PublicacionResponseDTO>> obtenerTodas(
            @RequestParam(required = false) String tipoTransaccion,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String tipoInmueble,
            @RequestParam(required = false) BigDecimal minPrecio,
            @RequestParam(required = false) BigDecimal maxPrecio) {
        
        if (tipoTransaccion == null && ubicacion == null && tipoInmueble == null && minPrecio == null && maxPrecio == null) {
            return ResponseEntity.ok(publicacionService.obtenerTodas());
        }
        
        return ResponseEntity.ok(publicacionService.obtenerTodasConFiltros(tipoTransaccion, ubicacion, tipoInmueble, minPrecio, maxPrecio));
    }

    @GetMapping("/mis-publicaciones")
    public ResponseEntity<List<PublicacionResponseDTO>> obtenerMisPublicaciones(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        return ResponseEntity.ok(publicacionService.obtenerPorUsuarioId(usuario.getId()));
    }

    @GetMapping("/mis-favoritos")
    public ResponseEntity<List<PublicacionResponseDTO>> obtenerMisFavoritos(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        return ResponseEntity.ok(publicacionService.obtenerMisFavoritos(usuario.getId()));
    }

    @PostMapping("/{id}/favorito")
    public ResponseEntity<Void> alternarFavorito(@PathVariable UUID id, Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        publicacionService.alternarFavorito(id, usuario.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicacionResponseDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(publicacionService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicacionResponseDTO> actualizar(@PathVariable UUID id,
            @RequestBody PublicacionRequestDTO dto) {
        return ResponseEntity.ok(publicacionService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        publicacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
