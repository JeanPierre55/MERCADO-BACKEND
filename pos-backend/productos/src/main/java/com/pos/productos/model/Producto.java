package com.pos.productos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Modelo de datos para un producto del catálogo POS.
 * Mapea directamente a un item de la tabla DynamoDB ProductosTable.
 */
public class Producto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("precio")
    private Double precio;

    // Constructor sin argumentos requerido por Jackson
    public Producto() {}

    public Producto(String id, String nombre, Double precio) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    @Override
    public String toString() {
        return "Producto{id='" + id + "', nombre='" + nombre + "', precio=" + precio + "}";
    }
}
