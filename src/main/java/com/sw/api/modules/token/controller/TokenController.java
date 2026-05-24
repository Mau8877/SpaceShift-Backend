package com.sw.api.modules.token.controller;

import com.sw.api.modules.token.model.PaqueteCredito;
import com.sw.api.modules.token.model.TransaccionCredito;
import com.sw.api.modules.token.repository.PaqueteCreditoRepository;
import com.sw.api.modules.token.service.TokenService;
import com.sw.api.modules.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final PaqueteCreditoRepository paqueteCreditoRepository;

    @GetMapping("/paquetes")
    public ResponseEntity<List<PaqueteCredito>> obtenerPaquetes() {
        return ResponseEntity.ok(paqueteCreditoRepository.findAll());
    }

    @GetMapping("/saldo")
    public ResponseEntity<Map<String, Object>> obtenerSaldo(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        int saldo = tokenService.obtenerSaldo(usuario.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("usuarioId", usuario.getId());
        response.put("saldoCreditos", saldo);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/historial")
    public ResponseEntity<Page<TransaccionCredito>> obtenerHistorial(
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        Page<TransaccionCredito> historial = tokenService.obtenerHistorial(usuario.getId(), pageable);
        return ResponseEntity.ok(historial);
    }
}
