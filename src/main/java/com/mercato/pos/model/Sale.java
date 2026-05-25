package com.mercato.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Representa una venta (transacción) en el sistema POS.
 * 
 * Una venta tiene un ciclo de vida: ACTIVE → COMPLETED/CANCELLED/FROZEN → ...
 * Todos los totales se almacenan en centavos (long) internamente.
 */
@Entity
@Table(name = "sales")
public class Sale {

    @Id
    private String id;

    @NotBlank(message = "El ID del terminal no puede estar vacío")
    private String terminalId;

    @NotBlank(message = "El ID del cajero no puede estar vacío")
    private String cashierId;

    private String customerId;

    @NotNull(message = "El estado no puede ser nulo")
    @Enumerated(EnumType.STRING)
    private SaleStatus status;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @NotNull(message = "El subtotal en centavos no puede ser nulo")
    @Min(value = 0, message = "El subtotal en centavos no puede ser negativo")
    private long subtotalCents;

    @NotNull(message = "El impuesto en centavos no puede ser nulo")
    @Min(value = 0, message = "El impuesto en centavos no puede ser negativo")
    private long taxCents;

    @NotNull(message = "El descuento en centavos no puede ser nulo")
    @Min(value = 0, message = "El descuento en centavos no puede ser negativo")
    private long discountCents;

    @NotNull(message = "El total en centavos no puede ser nulo")
    @Min(value = 0, message = "El total en centavos no puede ser negativo")
    private long totalCents;

    private String discountType;

    @DecimalMin(value = "0.00", inclusive = true, message = "El valor del descuento no puede ser negativo")
    @Column(precision = 19, scale = 2)
    private BigDecimal discountValue;

    @NotNull(message = "La tasa de impuesto no puede ser nula")
    @DecimalMin(value = "0.00", inclusive = true, message = "La tasa de impuesto no puede ser negativa")
    @Column(precision = 19, scale = 2)
    private BigDecimal taxRate;

    private String paymentType;

    private String creditReferenceNumber;

    @Column(unique = true)
    private String transactionId;

    private String cancellationReason;

    private LocalDateTime frozenAt;

    @NotNull(message = "La fecha de creación no puede ser nula")
    private LocalDateTime createdAt;

    @NotNull(message = "La fecha de actualización no puede ser nula")
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    // Constructores
    public Sale() {
        this.id = UUID.randomUUID().toString();
        this.status = SaleStatus.ACTIVE;
        this.subtotalCents = 0;
        this.taxCents = 0;
        this.discountCents = 0;
        this.totalCents = 0;
        this.taxRate = new BigDecimal("0.19"); // default 19%
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Sale(String terminalId, String cashierId) {
        this();
        this.terminalId = terminalId;
        this.cashierId = cashierId;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getCashierId() {
        return cashierId;
    }

    public void setCashierId(String cashierId) {
        this.cashierId = cashierId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public SaleStatus getStatus() {
        return status;
    }

    public void setStatus(SaleStatus status) {
        this.status = status;
    }

    public List<SaleItem> getItems() {
        return items;
    }

    public void setItems(List<SaleItem> items) {
        this.items = items;
    }

    public long getSubtotalCents() {
        return subtotalCents;
    }

    public void setSubtotalCents(long subtotalCents) {
        this.subtotalCents = subtotalCents;
    }

    public long getTaxCents() {
        return taxCents;
    }

    public void setTaxCents(long taxCents) {
        this.taxCents = taxCents;
    }

    public long getDiscountCents() {
        return discountCents;
    }

    public void setDiscountCents(long discountCents) {
        this.discountCents = discountCents;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public void setTotalCents(long totalCents) {
        this.totalCents = totalCents;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getCreditReferenceNumber() {
        return creditReferenceNumber;
    }

    public void setCreditReferenceNumber(String creditReferenceNumber) {
        this.creditReferenceNumber = creditReferenceNumber;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public LocalDateTime getFrozenAt() {
        return frozenAt;
    }

    public void setFrozenAt(LocalDateTime frozenAt) {
        this.frozenAt = frozenAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
