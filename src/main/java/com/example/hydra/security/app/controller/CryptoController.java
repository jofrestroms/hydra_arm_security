package com.example.hydra.security.app.controller;

import com.example.hydra.security.app.Services.EncryptionService;
import com.example.hydra.security.app.Utils.HashUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/cripto")
@CrossOrigin(origins = "*")
public class CryptoController {

    @Autowired
    private EncryptionService encryptionService;

    // Cambiamos a PostMapping para recibir el JSON en el cuerpo de la petición
    @GetMapping("/encrypt")
    public String encrypt(@RequestParam String texto) {
        return encryptionService.encriptarRobusto(texto);

    }
    
    @PostMapping("/encryptjson")
    public String encryptjson(@RequestBody String texto) {
        // Al usar @RequestBody, Spring leerá el JSON completo del cuerpo de la petición
        return encryptionService.encriptarRobusto(texto);
    }

    @GetMapping("/decrypt")
    public String decrypt(@RequestParam String codigo) {
        return encryptionService.Desencriptar(codigo);
    }

    /**
     * SHA-256 determinista de un RUN (minúsculas, sin puntos ni espacios).
     * A diferencia de /encrypt, este hasheo ES determinista: el mismo RUN
     * siempre devuelve el mismo hash. Se usa para que los clientes puedan
     * comparar su identidad contra los mensajes que llegan por SSE, ya que
     * el ciphertext nunca sale del servidor.
     */
    @GetMapping("/hash")
    public String hash(@RequestParam String run) {
        return HashUtils.HASHEO(run);
    }
}