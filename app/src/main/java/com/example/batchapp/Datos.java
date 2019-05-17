package com.example.batchapp;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Datos {
    private double longitudeNetwork;
    private double latitudeNetwork;
    private String titulo;
    private String mensaje;
    private String userId;
    private int icono;


    public Datos(double longitudeNetwork, double latitudeNetwork, String titulo, String mensaje, String userId, int icono) {
        this.longitudeNetwork = longitudeNetwork;
        this.latitudeNetwork = latitudeNetwork;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.userId = userId;
        this.icono = icono;
    }

    @Override
    public String toString() {
        return "Datos{" +
                "longitudeNetwork=" + longitudeNetwork +
                ", latitudeNetwork=" + latitudeNetwork +
                ", titulo='" + titulo + '\'' +
                ", mensaje='" + mensaje + '\'' +
                ", userId='" + userId + '\'' +
                ", icono='" + icono + '\'' +
                '}';
    }

    public double getLongitudeNetwork() {
        return longitudeNetwork;
    }

    public void setLongitudeNetwork(double longitudeNetwork) {
        this.longitudeNetwork = longitudeNetwork;
    }

    public double getLatitudeNetwork() {
        return latitudeNetwork;
    }

    public void setLatitudeNetwork(double latitudeNetwork) {
        this.latitudeNetwork = latitudeNetwork;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("longitudeNetwork", longitudeNetwork);
        result.put("latitudeNetwork", latitudeNetwork);
        result.put("titulo", titulo);
        result.put("mensaje", mensaje);
        result.put("icono", icono);

        return result;
    }

    public int getIcono() {
        return icono;
    }

    public void setIcono(int icono) {
        this.icono = icono;
    }
}
