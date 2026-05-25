package com.mercato.pos.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Representa un recibo generado al completar o devolver una venta.
 * 
 * El recibo contiene un snapshot JSON del estado completo de la transacción
 * para auditoría y consulta posterior.
 */
@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    private String id;

    @NotBlank(message = "El ID de transacción no puede estar vacío")
    @Column(unique = true, nullable = false)
    private String transactionId;

    @NotBlank(message = "El ID de venta no puede estar vacío")
    private String saleId;

    @NotBlank(message = "El tipo de recibo no puede estar vacío")
    private String receiptType;

    @NotNull(message = "El JSON del recibo no puede ser nulo")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String receiptJson;

    @NotNull(message = "La fecha de generación no puede ser nula")
    private LocalDateTime generatedAt;

    private String originalTransactionId;

    // Constructores
    public Receipt() {
        this.id = UUID.randomUUID().toString();
    }

    public Receipt(String transactionId, String saleId, String receiptType,
                   String receiptJson, LocalDateTime generatedAt) {
        this();
        this.transactionId = transactionId;
        this.saleId = saleId;
        this.receiptType = receiptType;
        this.receiptJson = receiptJson;
        this.generatedAt = generatedAt;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        this.saleId = saleId;
    }

    public String getReceiptType() {
        return receiptType;
    }

    public void setReceiptType(String receiptType) {
        this.receiptType = receiptType;
    }

    public String getReceiptJson() {
        return receiptJson;
    }

    public void setReceiptJson(String receiptJson) {
        this.receiptJson = receiptJson;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }
}
