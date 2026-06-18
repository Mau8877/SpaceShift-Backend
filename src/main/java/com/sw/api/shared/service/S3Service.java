package com.sw.api.shared.service;

import com.sw.api.modules.usuario.model.Perfil;
import com.sw.api.modules.usuario.repository.PerfilRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final PerfilRepository perfilRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    // Generar la estructura de carpeta del usuario: {idUsuario}/{nombre_apellido}
    public String resolverCarpetaUsuario(UUID usuarioId) {
        Perfil perfil = perfilRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el perfil del usuario para configurar S3."));

        String fullName = (perfil.getNombre() + " " + (perfil.getApellido() != null ? perfil.getApellido() : "")).trim();
        
        // Normalizar para remover acentos y diacríticos (ej: é -> e)
        String normalized = java.text.Normalizer.normalize(fullName, java.text.Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        String sanitizedName = withoutAccents.replaceAll("\\s+", "_")
                                              .replaceAll("[^a-zA-Z0-9_]", "")
                                              .toLowerCase();

        return usuarioId.toString() + "/" + sanitizedName;
    }

    // Generar URL firmada de subida (PUT)
    public S3PresignedResponse generatePresignedUploadUrl(UUID usuarioId, String filename, String contentType, String folder) {
        if (!folder.equals("videos") && !folder.equals("objeto3D") && !folder.equals("documento")) {
            throw new IllegalArgumentException("Carpeta inválida. Debe ser: 'videos', 'objeto3D' o 'documento'.");
        }

        String userFolder = resolverCarpetaUsuario(usuarioId);
        String uniqueFilename = userFolder + "/" + folder + "/" + UUID.randomUUID() + "_" + filename;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFilename)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15)) // URL válida por 15 mins
                .putObjectRequest(objectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, uniqueFilename);

        return new S3PresignedResponse(uploadUrl, publicUrl);
    }

    // Generar URL firmada para visualización de documentos privados (GET)
    public String generatePresignedGetUrl(String fileKey) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60)) // Enlace válido por 1 hora
                .getObjectRequest(objectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public record S3PresignedResponse(String uploadUrl, String fileUrl) {}
}
