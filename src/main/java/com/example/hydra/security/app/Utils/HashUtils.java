package com.example.hydra.security.app.Utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    /**
     * Genera un Hash SHA-256 para usar como nombre de carpeta.
     * @param rut El RUT original (ej: "12345678-9")
     * @return El Hash en hexadecimal (64 caracteres)
     */
    public static String HASHEO(String rut){
        try {
            
            String runLimpio = rut.replace(".", "").replace(" ", "").toLowerCase();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte [] ecodedhash = digest.digest(runLimpio.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * ecodedhash.length);
            for(byte b : ecodedhash){
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error crítico: No se encontró el algoritmo SHA-256", e);
        }
    }
    
}
