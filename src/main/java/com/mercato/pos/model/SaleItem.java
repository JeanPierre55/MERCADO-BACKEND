package com.mercato.pos.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Representa un item (línea de producto) dentro de una venta.
 * 
 * Cada item contiene un snapshot del precio al momento de agregarse a la venta,
 * de modo que cambios posteriores en la Product API no afecten la venta.
 */
@Entity
@Table(name = "sale_items")
public class SaleItem {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @NotBlank(message = "El ID del producto no puede estar vacío")
    private String productId;

    @NotBlank(message = "El nombre del producto no puede estar vacío")
    private String productName;

    private String barcode;

    @NotNull(message = "El precio unitario no puede ser nulo")
    @DecimalMin(value = "0.00", inclusive = true, message = "El precio unitario no puede ser negativo")
    @Column(precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @NotNull(message = "El precio unitario en centavos no puede ser nulo")
    @Min(value = 0, message = "El precio unitario en centavos no puede ser negativo")
    private long unitPriceCents;

    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
    private Integer quantity;

    @NotNull(message = "El total de línea no puede ser nulo")
    @DecimalMin(value = "0.00", inclusive = true, message = "El total de línea no puede ser negativo")
    @Column(precision = 19, scale = 2)
    private BigDecimal lineTotal;

    @NotNull(message = "El total de línea en centavos no puede ser nulo")
    @Min(value = 0, message = "El total de línea en centavos no puede ser negativo")
    private long lineTotalCents;

    // Constructores
    public SaleItem() {
        this.id = UUID.randomUUID().toString();
    }

    public SaleItem(String productId, String productName, String barcode,
                    BigDecimal unitPrice, long unitPriceCents, Integer quantity) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.barcode = barcode;
        this.unitPrice = unitPrice;
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public long getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(long unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public long getLineTotalCents() {
        return lineTotalCents;
    }

    public void setLineTotalCents(long lineTotalCents) {
        this.lineTotalCents = lineTotalCents;
    }
}
