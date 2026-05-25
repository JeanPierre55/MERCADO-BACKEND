package com.mercato.pos.controller;

import com.mercato.pos.dto.ReceiptResponse;
import com.mercato.pos.service.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para consultar recibos.
 */
@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    /**
     * Obtiene un recibo por su Transaction_ID.
     * 
     * @param transactionId ID de la transacción
     * @return respuesta con el recibo
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable String transactionId) {
        ReceiptResponse response = receiptService.getReceipt(transactionId);
        return ResponseEntity.ok(response);
    }
}
