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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

@Service
public class FotoPerfilService {

    @Value("${supabase.s3.endpoint}")
    private String endpoint;

    @Value("${supabase.s3.region}")
    private String region;

    @Value("${supabase.s3.access-key}")
    private String accessKey;

    @Value("${supabase.s3.secret-key}")
    private String secretKey;

    @Value("${supabase.bucket}")
    private String bucket;

    private S3Client getS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .build();
    }

    // ── Subir o reemplazar desde MultipartFile ──
    public String subirFotoPerfil(String rutOriginal, MultipartFile archivo) throws Exception {
        System.out.println("📤 RUT exacto al subir: '" + rutOriginal + "'");
        System.out.println("📤 RUT bytes: " + Arrays.toString(rutOriginal.getBytes(StandardCharsets.UTF_8)));
        String rutHasheado = HashUtils.HASHEO(rutOriginal);
        String carpeta = "pacientes/" + rutHasheado + "/perfil/";
        String path = carpeta + archivo.getOriginalFilename();

        limpiarCarpeta(carpeta);

        getS3Client().putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(path)
                        .contentType(archivo.getContentType())
                        .build(),
                RequestBody.fromBytes(archivo.getBytes()));

        System.out.println("✅ Foto de perfil subida: " + path);
        return construirUrlPublica(path);
    }

    // ── Subir o reemplazar desde Base64 ──
    public String subirFotoPerfilBase64(String rutOriginal, String nombreArchivo, String contenidoB64)
            throws Exception {
        String rutHasheado = HashUtils.HASHEO(rutOriginal);
        String carpeta = "pacientes/" + rutHasheado + "/perfil/";
        String path = carpeta + nombreArchivo;

        limpiarCarpeta(carpeta);

        String cleanB64 = contenidoB64.contains(",") ? contenidoB64.split(",")[1] : contenidoB64;
        byte[] data = Base64.getDecoder().decode(cleanB64);

        getS3Client().putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(path)
                        .contentType(detectarContentType(contenidoB64))
                        .build(),
                RequestBody.fromBytes(data));

        System.out.println("✅ Foto de perfil (base64) subida: " + path);
        return construirUrlPublica(path);
    }

    // ── Eliminar foto de perfil ──
    public void eliminarFotoPerfil(String rutOriginal) {
        String rutHasheado = HashUtils.HASHEO(rutOriginal);
        String carpeta = "pacientes/" + rutHasheado + "/perfil/";
        limpiarCarpeta(carpeta);
        System.out.println("🗑️ Foto de perfil eliminada para: " + rutOriginal);
    }

    // ── Elimina todos los archivos de la carpeta perfil ──
    private void limpiarCarpeta(String carpeta) {
        try {
            var listResponse = getS3Client().listObjectsV2(
                    ListObjectsV2Request.builder()
                            .bucket(bucket)
                            .prefix(carpeta)
                            .build());

            if (listResponse.contents().isEmpty())
                return;

            for (var objeto : listResponse.contents()) {
                getS3Client().deleteObject(
                        DeleteObjectRequest.builder()
                                .bucket(bucket)
                                .key(objeto.key())
                                .build());
                System.out.println("🗑️ Foto anterior eliminada: " + objeto.key());
            }
        } catch (Exception e) {
            System.out.println("Sin foto previa en: " + carpeta);
        }
    }

    private String construirUrlPublica(String path) {
        return endpoint.replace("/storage/v1/s3", "")
                + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    private String detectarContentType(String contenidoB64) {
        if (contenidoB64.startsWith("data:image/png"))
            return "image/png";
        if (contenidoB64.startsWith("data:image/jpeg"))
            return "image/jpeg";
        if (contenidoB64.startsWith("data:image/webp"))
            return "image/webp";
        if (contenidoB64.startsWith("data:image/gif"))
            return "image/gif";
        return "application/octet-stream";
    }

    public String obtenerFotoPorRut(String rutReal) {

        String rutHasheado = HashUtils.HASHEO(rutReal);
        String carpeta = "pacientes/" + rutHasheado + "/perfil/";

        System.out.println("🔍 Buscando en carpeta: " + carpeta);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(carpeta)
                .build();

        ListObjectsV2Response response = getS3Client().listObjectsV2(request);

        // 👇 Ver qué archivos encontró
        System.out.println("📦 Archivos encontrados: " + response.contents().size());
        response.contents().forEach(obj -> System.out.println("  → " + obj.key()));

        return response.contents().stream()
                .filter(obj -> !obj.key().equals(carpeta))
                .findFirst()
                .map(obj -> construirUrlPublica(obj.key()))
                .orElse(null);
    }
    public boolean tieneFotoPerfil(String rutOriginal) {
        try {
            String rutHasheado = HashUtils.HASHEO(rutOriginal);
            String carpeta = "pacientes/" + rutHasheado + "/perfil/";

            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(carpeta)
                    .build();

            ListObjectsV2Response response = getS3Client().listObjectsV2(request);

            boolean tiene = response.contents().stream()
                    .anyMatch(obj -> !obj.key().equals(carpeta));

            System.out.println(tiene ? "✅ Tiene foto: " + carpeta : "❌ Sin foto: " + carpeta);
            return tiene;

        } catch (Exception e) {
            System.err.println("❌ Error verificando foto: " + e.getMessage());
            return false;
        }
    }
}