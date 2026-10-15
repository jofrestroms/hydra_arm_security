package com.example.hydra.security.app.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Cifrado simetrico (Encryptors.text) del RUN que consume el APK.
 *
 * IMPORTANTE: este servicio DEBE compartir APP_CRYPTO_PASSWORD y APP_CRYPTO_SALT
 * con geolocalizaci-n, porque aquel descifra lo que este cifra.
 *
 * Si las claves no estan definidas el servicio ARRANCA igual: se desactiva el
 * cifrado (encrypt/decrypt devuelven error) y se registra por log, en vez de
 * abortar el arranque con un PlaceholderResolutionException. El hash SHA-256
 * (/api/user/cripto/hash) no necesita cifrado y sigue funcionando.
 *
 * El salt lo interpreta Encryptors.text como HEX, no como texto: debe ser una
 * cadena hexadecimal de longitud par.
 */
@Service
public class EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionService.class);

    @Value("${app.crypto.password}")
    private String password;

    @Value("${app.crypto.salt}")
    private String salt;


    private TextEncryptor encryptor;

    @PostConstruct
    public void Init(){
        if (password == null || password.isBlank() || salt == null || salt.isBlank()) {
            log.error("[security] APP_CRYPTO_PASSWORD / APP_CRYPTO_SALT no definidas. El servicio "
                    + "ARRANCARA, pero /encrypt y /decrypt devolveran error. El hash SHA-256 "
                    + "(/hash) sigue funcionando.");
            return;
        }
        if (!salt.matches("^[0-9a-fA-F]+$") || salt.length() % 2 != 0) {
            log.error("[security] APP_CRYPTO_SALT no es una cadena HEX de longitud par; "
                    + "se deshabilita el cifrado (Encryptors.text interpreta el salt como HEX).");
            return;
        }
        this.encryptor = Encryptors.text(password, salt);
        log.info("[security] Cifrado AES habilitado.");
    }

    public String encriptarRobusto(String datosO){
        if (this.encryptor == null) {
            throw new IllegalStateException(
                    "Cifrado deshabilitado: configure APP_CRYPTO_PASSWORD y APP_CRYPTO_SALT.");
        }
        String datoCR = datosO + "|" + System.currentTimeMillis();
        return encryptor.encrypt(datoCR);
    } 

    public String Desencriptar(String datosC){
        if (this.encryptor == null) {
            return "Error, no se pudo desencriptar";
        }
        try {
            String desencrptadoCR = encryptor.decrypt(datosC);
            return desencrptadoCR.split("\\|")[0];
        } catch (Exception e) {
            return "Error, no se pudo desencriptar";
        }
    }

}
