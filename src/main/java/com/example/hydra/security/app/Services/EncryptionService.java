package com.example.hydra.security.app.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class EncryptionService {
    
    @Value("${app.crypto.password}")
    private String password;

    @Value("${app.crypto.salt}")
    private String salt;
    

    private TextEncryptor encryptor;

    @PostConstruct
    public void Init(){
        this.encryptor = Encryptors.text(password, salt);
    }

    public String encriptarRobusto(String datosO){
        String datoCR = datosO + "|" + System.currentTimeMillis();
        return encryptor.encrypt(datoCR);
    } 

    public String Desencriptar(String datosC){
        try {
            String desencrptadoCR = encryptor.decrypt(datosC);
            return desencrptadoCR.split("\\|")[0];
        } catch (Exception e) {
            return "Error, no se pudo desencriptar";
        }
    }

}
