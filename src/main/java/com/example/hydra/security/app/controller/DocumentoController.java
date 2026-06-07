package com.example.hydra.security.app.controller;

import com.example.hydra.security.app.Services.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacientes/{runP}/documentos")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    // ── POST: subir documento ──
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> subirDocumento(
            @PathVariable String runP,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            String url = documentoService.subirDocumento(runP, archivo);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Documento subido correctamente",
                    "url", url));
        } catch (RuntimeException e) {
            // Nombre duplicado
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }

    // ── GET: listar documentos del paciente ──
    @GetMapping
    public ResponseEntity<Map<String, List<?>>> listarDocumentos(
            @PathVariable String runP) {
        try {
            List<DocumentoService.DocumentoInfo> documentos = documentoService.listarDocumentos(runP);
            // Ahora Map.of aceptará la lista de objetos sin quejarse
            return ResponseEntity.ok(Map.of("documentos", documentos));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", List.of(e.getMessage())));
        }
    }

    // ── DELETE: eliminar un documento específico ──
    @DeleteMapping("/{nombreArchivo}")
    public ResponseEntity<Map<String, String>> eliminarDocumento(
            @PathVariable String runP,
            @PathVariable String nombreArchivo) {
        try {
            documentoService.eliminarDocumento(runP, nombreArchivo);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Documento eliminado correctamente",
                    "archivo", nombreArchivo));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()));
        }
    }
}