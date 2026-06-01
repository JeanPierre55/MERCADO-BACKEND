package com.pos.ventas.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa un item de producto dentro de una solicitud de venta.
 * Contiene el ID del producto y la cantidad solicitada.
 */
public class ProductoItem {

    @JsonProperty("id")
    private String id;

    @JsonProperty("cantidad")
    private int cantidad;

    // Constructor sin argumentos requerido por Jackson
    public ProductoItem() {}

    public ProductoItem(String id, int cantidad) {
        this.id = id;
        this.cantidad = cantidad;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    @Override
    public String toString() {
        return "ProductoItem{id='" + id + "', cantidad=" + cantidad + "}";
    }
}
