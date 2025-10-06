package com.sistemagestionapp.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Bloque {

    private int indice;
    private String datos;
    private String hashAnterior;
    private String hash;

    public Bloque(int indice, String datos, String hashAnterior) {
        this.indice = indice;
        this.datos = datos;
        this.hashAnterior = hashAnterior;
        this.hash = calcularHash();
    }

    public String calcularHash() {
        try {
            String contenido = indice + datos + hashAnterior;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    // Getters
    public int getIndice() {
        return indice;
    }

    public String getDatos() {
        return datos;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public String getHash() {
        return hash;
    }
}