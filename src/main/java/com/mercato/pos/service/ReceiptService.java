package com.mercato.pos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercato.pos.dto.ReceiptResponse;
import com.mercato.pos.exception.ReceiptNotFoundException;
import com.mercato.pos.model.Receipt;
import com.mercato.pos.model.Sale;
import com.mercato.pos.repository.ReceiptRepository;
import com.mercato.pos.util.MoneyCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para generar y consultar recibos de venta y devolución.
 */
@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ObjectMapper objectMapper;

    public ReceiptService(ReceiptRepository receiptRepository, ObjectMapper objectMapper) {
        this.receiptRepository = receiptRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Genera un recibo de venta.
     * 
     * @param sale la venta completada
     * @param amountReceived monto recibido (para CASH)
     * @return recibo generado
     */
    @Transactional
    public Receipt generateSaleReceipt(Sale sale, BigDecimal amountReceived) {
        String transactionId = UUID.randomUUID().toString();
        
        Map<String, Object> receiptData = new HashMap<>();
        receiptData.put("transactionId", transactionId);
        receiptData.put("storeName", "MERCATO POS");
        receiptData.put("terminalId", sale.getTerminalId());
        receiptData.put("cashierId", sale.getCashierId());
        receiptData.put("customerId", sale.getCustomerId());
        receiptData.put("generatedAt", LocalDateTime.now());
        
        // Items
        var items = sale.getItems().stream()
            .map(item -> Map.of(
                "productId", item.getProductId(),
                "productName", item.getProductName(),
                "quantity", item.getQuantity(),
                "unitPrice", item.getUnitPrice(),
                "lineTotal", item.getLineTotal()
            ))
            .toList();
        receiptData.put("items", items);
        
        // Totales
        receiptData.put("subtotal", MoneyCalculator.fromCents(sale.getSubtotalCents()));
        receiptData.put("tax", MoneyCalculator.fromCents(sale.getTaxCents()));
        receiptData.put("discount", MoneyCalculator.fromCents(sale.getDiscountCents()));
        receiptData.put("total", MoneyCalculator.fromCents(sale.getTotalCents()));
        
        // Pago
        receiptData.put("paymentType", sale.getPaymentType());
        if ("CASH".equals(sale.getPaymentType()) && amountReceived != null) {
            receiptData.put("amountReceived", amountReceived);
            BigDecimal change = amountReceived.subtract(MoneyCalculator.fromCents(sale.getTotalCents()));
            receiptData.put("change", change);
        } else if ("CREDIT".equals(sale.getPaymentType())) {
            receiptData.put("creditReferenceNumber", sale.getCreditReferenceNumber());
        }
        
        try {
            String receiptJson = objectMapper.writeValueAsString(receiptData);
            
            Receipt receipt = new Receipt();
            receipt.setId(UUID.randomUUID().toString());
            receipt.setTransactionId(transactionId);
            receipt.setSaleId(sale.getId());
            receipt.setReceiptType("SALE");
            receipt.setReceiptJson(receiptJson);
            receipt.setGeneratedAt(LocalDateTime.now());
            
            // Actualizar la venta con el transactionId
            sale.setTransactionId(transactionId);
            sale.setCompletedAt(LocalDateTime.now());
            
            return receiptRepository.save(receipt);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar recibo de venta", e);
        }
    }

    /**
     * Genera un recibo de devolución.
     * 
     * @param sale la venta devuelta
     * @param returnReason razón de la devolución
     * @param returnType tipo de devolución (RETURN o PARTIAL_RETURN)
     * @return recibo de devolución generado
     */
    @Transactional
    public Receipt generateReturnReceipt(Sale sale, String returnReason, String returnType) {
        String transactionId = UUID.randomUUID().toString();
        
        Map<String, Object> receiptData = new HashMap<>();
        receiptData.put("transactionId", transactionId);
        receiptData.put("originalTransactionId", sale.getTransactionId());
        receiptData.put("storeName", "MERCATO POS");
        receiptData.put("terminalId", sale.getTerminalId());
        receiptData.put("cashierId", sale.getCashierId());
        receiptData.put("returnedAt", LocalDateTime.now());
        receiptData.put("returnReason", returnReason);
        
        // Items devueltos
        var items = sale.getItems().stream()
            .map(item -> Map.of(
                "productId", item.getProductId(),
                "productName", item.getProductName(),
                "quantity", item.getQuantity(),
                "unitPrice", item.getUnitPrice(),
                "lineTotal", item.getLineTotal()
            ))
            .toList();
        receiptData.put("items", items);
        
        // Totales
        receiptData.put("subtotal", MoneyCalculator.fromCents(sale.getSubtotalCents()));
        receiptData.put("tax", MoneyCalculator.fromCents(sale.getTaxCents()));
        receiptData.put("discount", MoneyCalculator.fromCents(sale.getDiscountCents()));
        receiptData.put("totalReturned", MoneyCalculator.fromCents(sale.getTotalCents()));
        
        // Pago original
        receiptData.put("originalPaymentType", sale.getPaymentType());
        if ("CREDIT".equals(sale.getPaymentType())) {
            receiptData.put("creditReferenceNumber", sale.getCreditReferenceNumber());
        }
        
        try {
            String receiptJson = objectMapper.writeValueAsString(receiptData);
            
            Receipt receipt = new Receipt();
            receipt.setId(UUID.randomUUID().toString());
            receipt.setTransactionId(transactionId);
            receipt.setSaleId(sale.getId());
            receipt.setReceiptType(returnType);
            receipt.setReceiptJson(receiptJson);
            receipt.setGeneratedAt(LocalDateTime.now());
            receipt.setOriginalTransactionId(sale.getTransactionId());
            
            return receiptRepository.save(receipt);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar recibo de devolución", e);
        }
    }

    /**
     * Obtiene un recibo por su Transaction_ID.
     * 
     * @param transactionId ID de la transacción
     * @return respuesta con el recibo
     * @throws ReceiptNotFoundException si el recibo no existe
     */
    public ReceiptResponse getReceipt(String transactionId) {
        Receipt receipt = receiptRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new ReceiptNotFoundException("Recibo no encontrado: " + transactionId));
        
        try {
            Object receiptData = objectMapper.readValue(receipt.getReceiptJson(), Object.class);
            return new ReceiptResponse(
                receipt.getTransactionId(),
                extractCreditReference(receiptData),
                receipt.getReceiptType(),
                receiptData
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al deserializar recibo", e);
        }
    }

    /**
     * Extrae el número de referencia de crédito del recibo si existe.
     */
    private String extractCreditReference(Object receiptData) {
        if (receiptData instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) receiptData;
            Object ref = map.get("creditReferenceNumber");
            return ref != null ? ref.toString() : null;
        }
        return null;
    }
}
