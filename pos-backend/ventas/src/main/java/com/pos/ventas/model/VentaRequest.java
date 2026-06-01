package com.pos.ventas.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO que representa el body del request POST /ventas.
 * Contiene la lista de productos a registrar en la venta.
 */
public class VentaRequest {

    @JsonProperty("productos")
    private List<ProductoItem> productos;

    // Constructor sin argumentos requerido por Jackson
    public VentaRequest() {}

    public VentaRequest(List<ProductoItem> productos) {
        this.productos = productos;
    }

    public List<ProductoItem> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoItem> productos) {
        this.productos = productos;
    }

    @Override
    public String toString() {
        return "VentaRequest{productos=" + productos + "}";
    }
}
