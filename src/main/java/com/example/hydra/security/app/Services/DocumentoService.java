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
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.time.Instant; // Importante para la fecha
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentoService {

    // --- Tu configuración de @Value se mantiene igual ---
    @Value("${supabase.s3.endpoint}") private String endpoint;
    @Value("${supabase.s3.region}") private String region;
    @Value("${supabase.s3.access-key}") private String accessKey;
    @Value("${supabase.s3.secret-key}") private String secretKey;
    @Value("${supabase.bucket}") private String bucket;

    // --- Clase DTO interna para la respuesta ---
    public static class DocumentoInfo {
        private String nombre;
        private String url;
        private Instant fechaCreacion;

        public DocumentoInfo(String nombre, String url, Instant fechaCreacion) {
            this.nombre = nombre;
            this.url = url;
            this.fechaCreacion = fechaCreacion;
        }
        // Getters (Necesarios para que Spring los convierta a JSON)
        public String getNombre() { return nombre; }
        public String getUrl() { return url; }
        public Instant getFechaCreacion() { return fechaCreacion; }
    }

    private S3Client getS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .forcePathStyle(true)
                .build();
    }

    // --- LISTAR DOCUMENTOS (MODIFICADO) ---
    public List<DocumentoInfo> listarDocumentos(String rutOriginal) {
        String rutHasheado = HashUtils.HASHEO(rutOriginal);
        String carpeta = "pacientes/" + rutHasheado + "/documentos/";

        var listResponse = getS3Client().listObjectsV2(
                ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(carpeta)
                        .build()
        );

        return listResponse.contents()
                .stream()
                .filter(obj -> obj.size() > 0) // Evita carpetas vacías si las hay
                .map(obj -> {
                    String fullPath = obj.key();
                    // Extraemos solo el nombre del archivo del path completo
                    String nombreArchivo = fullPath.substring(fullPath.lastIndexOf("/") + 1);
                    
                    return new DocumentoInfo(
                        nombreArchivo,
                        construirUrlPublica(fullPath),
                        obj.lastModified() // <--- Este es el "Added on"
                    );
                })
                .collect(Collectors.toList());
    }

    // --- El resto de tus métodos (subir, eliminar, existe, construirUrl) se mantienen igual ---
    
    public String subirDocumento(String rutOriginal, MultipartFile archivo) throws Exception {
        String rutHasheado = HashUtils.HASHEO(rutOriginal);
        String path = "pacientes/" + rutHasheado + "/documentos/" + archivo.getOriginalFilename();

        if (existeArchivo(path)) {
            throw new RuntimeException("Ya existe un documento con ese nombre.");
        }

        getS3Client().putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(path)
                        .contentType(archivo.getContentType())
                        .build(),
                RequestBody.fromBytes(archivo.getBytes())
        );
        return construirUrlPublica(path);
    }

    public void eliminarDocumento(String rutOriginal, String nombreArchivo) {
        String rutHasheado = HashUtils.HASHEO(rutOriginal);
        String path = "pacientes/" + rutHasheado + "/documentos/" + nombreArchivo;
        getS3Client().deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(path).build());
    }

    private boolean existeArchivo(String path) {
        try {
            getS3Client().headObject(HeadObjectRequest.builder().bucket(bucket).key(path).build());
            return true;
        } catch (NoSuchKeyException e) { return false; }
    }

    private String construirUrlPublica(String path) {
        return endpoint.substring(0, endpoint.indexOf(".storage.supabase.co"))
                + ".supabase.co/storage/v1/object/public/" + bucket + "/" + path;
    }
}