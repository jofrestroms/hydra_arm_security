package com.example.hydra.security.app.Services;

import org.springframework.stereotype.Service;

@Service
public class RecetaServices {

    public String generarTextoReceta(String medicamentos, String indicaciones) {
        return "Receta Medica\n\n" +
               "Medicamentos: " + medicamentos + "\n" +
               "Indicaciones: " + indicaciones;
    }

    public boolean validarReceta(String medicamentos, String indicaciones) {
        return medicamentos != null && !medicamentos.trim().isEmpty() &&
               indicaciones != null && !indicaciones.trim().isEmpty();
    }
}
