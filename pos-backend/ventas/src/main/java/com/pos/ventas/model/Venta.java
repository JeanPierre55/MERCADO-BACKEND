package com.pos.ventas.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Modelo de datos para una venta registrada en el sistema POS.
 * Mapea directamente a un item de la tabla DynamoDB VentasTable.
 */
public class Venta {

    @JsonProperty("id")
    private String id;

    @JsonProperty("productos")
    private List<ProductoItem> productos;

    @JsonProperty("timestamp")
    private String timestamp;

    // Constructor sin argumentos requerido por Jackson
    public Venta() {}

    public Venta(String id, List<ProductoItem> productos, String timestamp) {
        this.id = id;
        this.productos = productos;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ProductoItem> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoItem> productos) {
        this.productos = productos;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Venta{id='" + id + "', productos=" + productos + ", timestamp='" + timestamp + "'}";
    }
}
