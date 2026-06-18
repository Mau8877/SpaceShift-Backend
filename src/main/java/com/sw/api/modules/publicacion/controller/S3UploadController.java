package com.sw.api.modules.publicacion.controller;

import com.sw.api.modules.usuario.model.Usuario;
import com.sw.api.shared.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/upload/s3")
@RequiredArgsConstructor
public class S3UploadController {

    private final S3Service s3Service;

    @GetMapping("/presigned-url")
    public ResponseEntity<S3Service.S3PresignedResponse> obtenerPresignedUrl(
            @RequestParam String filename,
            @RequestParam String contentType,
            @RequestParam String folder,
            Authentication authentication) {
        
        Usuario usuario = (Usuario) authentication.getPrincipal();
        S3Service.S3PresignedResponse response = s3Service.generatePresignedUploadUrl(
                usuario.getId(),
                filename,
                contentType,
                folder
        );
        return ResponseEntity.ok(response);
    }
}
