package com.example.hydra.security.app.controller;

import com.example.hydra.security.app.Services.EncryptionService;
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
}