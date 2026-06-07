package com.example.hydra.security.app.controller;

import com.example.hydra.security.app.Services.FotoPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/pacientes/{runP:.+}/perfil")
public class FotoPerfilController {

    @Autowired
    private FotoPerfilService fotoPerfilService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> subirFoto(
            @PathVariable String runP,
            @RequestParam("imagen") MultipartFile archivo) {
        try {
            String url = fotoPerfilService.subirFotoPerfil(runP, archivo);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Foto de perfil subida correctamente",
                    "url", url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }

    @PostMapping("/base64")
    public ResponseEntity<Map<String, String>> subirFotoBase64(
            @PathVariable String runP,
            @RequestBody Map<String, String> body) {
        try {
            String nombreArchivo = body.get("nombreArchivo");
            String contenidoB64 = body.get("contenido");

            if (nombreArchivo == null || contenidoB64 == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Se requieren 'nombreArchivo' y 'contenido'"));
            }

            String url = fotoPerfilService.subirFotoPerfilBase64(runP, nombreArchivo, contenidoB64);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Foto de perfil subida correctamente",
                    "url", url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> eliminarFoto(
            @PathVariable String runP) {
        try {
            fotoPerfilService.eliminarFotoPerfil(runP);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Foto de perfil eliminada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> obtenerFotoPorRut(
            @PathVariable String runP) {
        try {
            String url = fotoPerfilService.obtenerFotoPorRut(runP);

            // ✅ Si no tiene foto, devolver 404 en vez de explotar
            if (url == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Map.of("url", url));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }


}