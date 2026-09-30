package com.example.hydra.security.app.Services;

import com.example.hydra.security.app.Utils.HashUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

/**
 * Adjuntos de mensajeria en Supabase Storage (bucket "Mensajes").
 * Estructura: conversaciones/&lt;SHA256(runMenor)&gt;_&lt;SHA256(runMayor)&gt;/&lt;archivo&gt;
 * La carpeta es deterministica e igual en ambas direcciones de la conversacion 1:1,
 * y los runs nunca aparecen en la URL (solo su hash).
 */
@Service
public class AdjuntoMensajeService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    @Value("${supabase.s3.endpoint}") private String endpoint;
    @Value("${supabase.s3.region}") private String region;
    @Value("${supabase.s3.access-key}") private String accessKey;
    @Value("${supabase.s3.secret-key}") private String secretKey;
    @Value("${supabase.bucket.mensajes}") private String bucket;

    public static class AdjuntoInfo {
        private final String url;
        private final String nombre;

        public AdjuntoInfo(String url, String nombre) {
            this.url = url;
            this.nombre = nombre;
        }

        public String getUrl() { return url; }
        public String getNombre() { return nombre; }
    }

    private S3Client getS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .build();
    }

    public AdjuntoInfo subirAdjunto(String runA, String runB, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new RuntimeException("El archivo está vacío.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new RuntimeException("El archivo supera el límite de 10 MB.");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !contentTypePermitido(tipo)) {
            throw new RuntimeException("Tipo de archivo no permitido.");
        }

        String nombre = sanearNombre(archivo.getOriginalFilename());
        String key = carpetaConversacion(runA, runB) + nombre;

        try {
            getS3Client().putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(tipo)
                            .build(),
                    RequestBody.fromBytes(archivo.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error subiendo adjunto: " + e.getMessage());
        }
        return new AdjuntoInfo(construirUrlPublica(key), nombre);
    }

    public void eliminarAdjunto(String url) {
        String key = extraerKey(url);
        if (key == null) {
            throw new RuntimeException("URL de adjunto inválida.");
        }
        getS3Client().deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private String carpetaConversacion(String runA, String runB) {
        String a = normalizarRun(runA);
        String b = normalizarRun(runB);
        String menor = a.compareTo(b) <= 0 ? a : b;
        String mayor = a.compareTo(b) <= 0 ? b : a;
        return "conversaciones/" + HashUtils.HASHEO(menor) + "_" + HashUtils.HASHEO(mayor) + "/";
    }

    private String normalizarRun(String run) {
        return run == null ? "" : run.replace(".", "").replace(" ", "").toLowerCase().trim();
    }

    private String sanearNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "archivo";
        }
        String limpio = nombre.replaceAll("[\\\\/]", "_").replaceAll("[\\p{Cntrl}]", "").trim();
        if (limpio.isBlank() || limpio.length() > 255) {
            return "archivo";
        }
        return limpio;
    }

    private boolean contentTypePermitido(String tipo) {
        return tipo.startsWith("image/")
                || tipo.startsWith("text/")
                || tipo.equals("application/pdf")
                || tipo.equals("application/msword")
                || tipo.startsWith("application/vnd.openxmlformats-officedocument.")
                || tipo.equals("application/octet-stream");
    }

    private String construirUrlPublica(String key) {
        return endpoint.substring(0, endpoint.indexOf(".storage.supabase.co"))
                + ".supabase.co/storage/v1/object/public/" + bucket + "/" + key;
    }

    private String extraerKey(String url) {
        if (url == null) {
            return null;
        }
        String prefijo = bucket + "/";
        int idx = url.indexOf(prefijo);
        if (idx < 0) {
            return null;
        }
        String key = url.substring(idx + prefijo.length());
        if (key.contains("?")) {
            key = key.substring(0, key.indexOf("?"));
        }
        return key.isEmpty() ? null : key;
    }
}