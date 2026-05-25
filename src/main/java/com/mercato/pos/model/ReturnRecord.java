package com.mercato.pos.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Representa un registro de devolución de un producto en una venta.
 * 
 * Se utiliza para rastrear devoluciones parciales y totales de items.
 */
@Entity
@Table(name = "return_records")
public class ReturnRecord {

    @Id
    private String id;

    @NotBlank(message = "El ID de venta no puede estar vacío")
    private String saleId;

    @NotBlank(message = "El ID del producto no puede estar vacío")
    private String productId;

    @NotNull(message = "La cantidad devuelta no puede ser nula")
    @Min(value = 1, message = "La cantidad devuelta debe ser mayor o igual a 1")
    private Integer quantityReturned;

    @NotBlank(message = "La razón de devolución no puede estar vacía")
    private String returnReason;

    @NotNull(message = "La fecha de devolución no puede ser nula")
    private LocalDateTime returnedAt;

    private String receiptId;

    // Constructores
    public ReturnRecord() {
        this.id = UUID.randomUUID().toString();
    }

    public ReturnRecord(String saleId, String productId, Integer quantityReturned,
                        String returnReason, LocalDateTime returnedAt) {
        this();
        this.saleId = saleId;
        this.productId = productId;
        this.quantityReturned = quantityReturned;
        this.returnReason = returnReason;
        this.returnedAt = returnedAt;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        this.saleId = saleId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantityReturned() {
        return quantityReturned;
    }

    public void setQuantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }
}
