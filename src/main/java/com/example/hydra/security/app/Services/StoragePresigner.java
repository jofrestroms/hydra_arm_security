package com.example.hydra.security.app.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

/**
 * Genera URLs firmadas (presigned) de descarga para Supabase Storage.
 *
 * Los buckets son privados: no existe el acceso publico
 * (.../object/public/...). Cada enlace caduca a los supabase.s3.presign-minutes
 * minutos, asi que se regenera bajo demanda (listado de documentos, foto de
 * perfil) o via el endpoint estable de adjuntos que redirige a una firmada.
 */
@Service
public class StoragePresigner {

    @Value("${supabase.s3.endpoint}") private String endpoint;
    @Value("${supabase.s3.region}") private String region;
    @Value("${supabase.s3.access-key}") private String accessKey;
    @Value("${supabase.s3.secret-key}") private String secretKey;
    @Value("${supabase.s3.presign-minutes:60}") private long presignMinutes;
    @Value("${app.public-base-url:}") private String publicBaseUrl;

    /**
     * @param contentDisposition opcional; ej. "attachment; filename=\"examen.pdf\""
     *                           para forzar descarga, o null para mostrar inline.
     */
    public String presignGet(String bucket, String key, String contentDisposition) {
        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build()) {

            GetObjectRequest.Builder get = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key);
            if (contentDisposition != null && !contentDisposition.isBlank()) {
                get.responseContentDisposition(contentDisposition);
            }

            PresignedGetObjectRequest presigned = presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofMinutes(presignMinutes))
                            .getObjectRequest(get.build())
                            .build());

            return presigned.url().toString();
        }
    }

    /** URL publica base de este servicio, sin barra final. */
    public String publicBaseUrl() {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return "";
        }
        return publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }
}
