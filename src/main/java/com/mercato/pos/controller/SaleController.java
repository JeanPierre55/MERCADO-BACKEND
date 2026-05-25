package com.mercato.pos.controller;

import com.mercato.pos.dto.AddItemRequest;
import com.mercato.pos.dto.CancelSaleRequest;
import com.mercato.pos.dto.CheckoutRequest;
import com.mercato.pos.dto.CreateSaleRequest;
import com.mercato.pos.dto.PartialReturnRequest;
import com.mercato.pos.dto.ReceiptResponse;
import com.mercato.pos.dto.ReturnRequest;
import com.mercato.pos.dto.SaleResponse;
import com.mercato.pos.dto.UpdateItemRequest;
import com.mercato.pos.service.FreezeService;
import com.mercato.pos.service.PaymentService;
import com.mercato.pos.service.ReturnService;
import com.mercato.pos.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para gestionar ventas.
 */
@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;
    private final PaymentService paymentService;
    private final FreezeService freezeService;
    private final ReturnService returnService;

    public SaleController(SaleService saleService,
                          PaymentService paymentService,
                          FreezeService freezeService,
                          ReturnService returnService) {
        this.saleService = saleService;
        this.paymentService = paymentService;
        this.freezeService = freezeService;
        this.returnService = returnService;
    }

    /**
     * Crea una nueva venta.
     * 
     * @param request datos de la solicitud
     * @param authentication información del usuario autenticado
     * @return respuesta con la venta creada
     */
    @PostMapping
    public ResponseEntity<SaleResponse> createSale(
            @Valid @RequestBody CreateSaleRequest request,
            Authentication authentication) {
        String cashierId = authentication.getName();
        SaleResponse response = saleService.createSale(request, cashierId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene una venta por su ID.
     * 
     * @param saleId ID de la venta
     * @return respuesta con la venta
     */
    @GetMapping("/{saleId}")
    public ResponseEntity<SaleResponse> getSale(@PathVariable String saleId) {
        SaleResponse response = saleService.getSale(saleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Agrega un item a una venta.
     * 
     * @param saleId ID de la venta
     * @param request datos del item
     * @return respuesta con la venta actualizada
     */
    @PostMapping("/{saleId}/items")
    public ResponseEntity<SaleResponse> addItem(
            @PathVariable String saleId,
            @Valid @RequestBody AddItemRequest request) {
        SaleResponse response = saleService.addItem(saleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza la cantidad de un item en una venta.
     * 
     * @param saleId ID de la venta
     * @param itemId ID del item
     * @param request datos de actualización
     * @return respuesta con la venta actualizada
     */
    @PutMapping("/{saleId}/items/{itemId}")
    public ResponseEntity<SaleResponse> updateItem(
            @PathVariable String saleId,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        SaleResponse response = saleService.updateItem(saleId, itemId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina un item de una venta.
     * 
     * @param saleId ID de la venta
     * @param itemId ID del item
     * @return respuesta con la venta actualizada
     */
    @DeleteMapping("/{saleId}/items/{itemId}")
    public ResponseEntity<SaleResponse> removeItem(
            @PathVariable String saleId,
            @PathVariable String itemId) {
        SaleResponse response = saleService.removeItem(saleId, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancela una venta.
     * 
     * @param saleId ID de la venta
     * @param request datos de cancelación
     * @return respuesta con la venta cancelada
     */
    @PostMapping("/{saleId}/cancel")
    public ResponseEntity<SaleResponse> cancelSale(
            @PathVariable String saleId,
            @Valid @RequestBody CancelSaleRequest request) {
        SaleResponse response = saleService.cancelSale(saleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Procesa el checkout de una venta (CASH o CREDIT).
     * 
     * @param saleId ID de la venta
     * @param request datos del checkout
     * @return respuesta con el recibo generado
     */
    @PostMapping("/{saleId}/checkout")
    public ResponseEntity<ReceiptResponse> checkout(
            @PathVariable String saleId,
            @Valid @RequestBody CheckoutRequest request) {
        ReceiptResponse response = paymentService.checkout(saleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Congela una venta (pausa temporal).
     * 
     * @param saleId ID de la venta
     * @return respuesta con la venta congelada
     */
    @PostMapping("/{saleId}/freeze")
    public ResponseEntity<SaleResponse> freezeSale(@PathVariable String saleId) {
        SaleResponse response = freezeService.freezeSale(saleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reanuda una venta congelada.
     * 
     * @param saleId ID de la venta
     * @return respuesta con la venta reanudada
     */
    @PostMapping("/{saleId}/resume")
    public ResponseEntity<SaleResponse> resumeSale(@PathVariable String saleId) {
        SaleResponse response = freezeService.resumeSale(saleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene todas las ventas congeladas de un terminal.
     * 
     * @param terminalId ID del terminal
     * @return lista de ventas congeladas
     */
    @GetMapping("/frozen")
    public ResponseEntity<List<SaleResponse>> getFrozenSales(@RequestParam String terminalId) {
        List<SaleResponse> response = freezeService.getFrozenSales(terminalId);
        return ResponseEntity.ok(response);
    }

    /**
     * Procesa la devolución total de una venta.
     * 
     * @param saleId ID de la venta
     * @param request datos de la devolución
     * @return respuesta con el recibo de devolución
     */
    @PostMapping("/{saleId}/return")
    public ResponseEntity<ReceiptResponse> fullReturn(
            @PathVariable String saleId,
            @Valid @RequestBody ReturnRequest request) {
        ReceiptResponse response = returnService.fullReturn(saleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Procesa la devolución parcial de items específicos de una venta.
     * 
     * @param saleId ID de la venta
     * @param request datos de la devolución parcial
     * @return respuesta con el recibo de devolución parcial
     */
    @PostMapping("/{saleId}/return/partial")
    public ResponseEntity<ReceiptResponse> partialReturn(
            @PathVariable String saleId,
            @Valid @RequestBody PartialReturnRequest request) {
        ReceiptResponse response = returnService.partialReturn(saleId, request);
        return ResponseEntity.ok(response);
    }
}
