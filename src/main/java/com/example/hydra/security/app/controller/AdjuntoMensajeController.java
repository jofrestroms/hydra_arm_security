package com.example.hydra.security.app.controller;

import com.example.hydra.security.app.Services.AdjuntoMensajeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/mensajes/adjuntos")
public class AdjuntoMensajeController {

    @Autowired
    private AdjuntoMensajeService adjuntoMensajeService;

    /**
     * Sube un adjunto para una conversacion 1:1.
     * multipart/form-data: archivo + runA + runB
     * Devuelve { url, nombre } listos para guardar en /api/mensajes.
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> subirAdjunto(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("runA") String runA,
            @RequestParam("runB") String runB) {
        try {
            AdjuntoMensajeService.AdjuntoInfo info = adjuntoMensajeService.subirAdjunto(runA, runB, archivo);
            return ResponseEntity.ok(Map.of(
                    "url", info.getUrl(),
                    "nombre", info.getNombre()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Elimina un adjunto: DELETE /api/mensajes/adjuntos?url=... */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> eliminar(@RequestParam("url") String url) {
        try {
            adjuntoMensajeService.eliminarAdjunto(url);
            return ResponseEntity.ok(Map.of("mensaje", "adjunto eliminado"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}