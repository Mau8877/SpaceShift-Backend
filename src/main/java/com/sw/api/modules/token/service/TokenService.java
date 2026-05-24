package com.sw.api.modules.token.service;

import com.sw.api.modules.token.model.TipoTransaccion;
import com.sw.api.modules.token.model.TransaccionCredito;
import com.sw.api.modules.token.repository.TransaccionCreditoRepository;
import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import com.sw.api.modules.usuario.repository.UsuarioRepository;
import com.sw.api.modules.publicacion.model.Publicacion;
import com.sw.api.modules.publicacion.repository.PublicacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final PerfilRepository perfilRepository;
    private final TransaccionCreditoRepository transaccionCreditoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PublicacionRepository publicacionRepository;

    @Transactional(readOnly = true)
    public int obtenerSaldo(UUID usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId)
                .map(Perfil::getSaldoCreditos)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public boolean verificarSaldo(UUID usuarioId, int costo) {
        return obtenerSaldo(usuarioId) >= costo;
    }

    @Transactional
    public void debitarCreditos(UUID usuarioId, int cantidad, UUID publicacionId, String descripcion) {
        Perfil perfil = perfilRepository.findByUsuarioIdForUpdate(usuarioId)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado para el usuario."));

        if (perfil.getSaldoCreditos() < cantidad) {
            throw new RuntimeException("Créditos insuficientes. Saldo actual: " + perfil.getSaldoCreditos() + ", Requerido: " + cantidad);
        }

        perfil.setSaldoCreditos(perfil.getSaldoCreditos() - cantidad);
        perfilRepository.save(perfil);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        Publicacion publicacion = null;
        if (publicacionId != null) {
            publicacion = publicacionRepository.findById(publicacionId).orElse(null);
        }

        TransaccionCredito transaccion = TransaccionCredito.builder()
                .usuario(usuario)
                .cantidad(-cantidad)
                .tipo(TipoTransaccion.CONSUMO_PROCESAMIENTO)
                .descripcion(descripcion)
                .publicacion(publicacion)
                .build();

        transaccionCreditoRepository.save(transaccion);
    }

    @Transactional
    public TransaccionCredito acreditarCreditos(UUID usuarioId, int cantidad, String descripcion, TipoTransaccion tipo) {
        Perfil perfil = perfilRepository.findByUsuarioIdForUpdate(usuarioId)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado para el usuario."));

        perfil.setSaldoCreditos(perfil.getSaldoCreditos() + cantidad);
        perfilRepository.save(perfil);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        TransaccionCredito transaccion = TransaccionCredito.builder()
                .usuario(usuario)
                .cantidad(cantidad)
                .tipo(tipo)
                .descripcion(descripcion)
                .build();

        return transaccionCreditoRepository.save(transaccion);
    }

    @Transactional(readOnly = true)
    public Page<TransaccionCredito> obtenerHistorial(UUID usuarioId, Pageable pageable) {
        return transaccionCreditoRepository.findByUsuarioIdOrderByCreatedDateDesc(usuarioId, pageable);
    }
}
