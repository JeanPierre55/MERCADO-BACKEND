package com.mercato.pos.service;

import com.mercato.pos.dto.PartialReturnRequest;
import com.mercato.pos.dto.ReceiptResponse;
import com.mercato.pos.dto.ReturnItemRequest;
import com.mercato.pos.dto.ReturnRequest;
import com.mercato.pos.exception.SaleAlreadyReturnedException;
import com.mercato.pos.exception.SaleNotFoundException;
import com.mercato.pos.exception.SaleNotCompletedException;
import com.mercato.pos.model.ReturnRecord;
import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleItem;
import com.mercato.pos.model.SaleStatus;
import com.mercato.pos.repository.ReturnRecordRepository;
import com.mercato.pos.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para procesar devoluciones totales y parciales.
 */
@Service
public class ReturnService {

    private final SaleRepository saleRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final ProductClientService productClientService;
    private final ReceiptService receiptService;

    public ReturnService(SaleRepository saleRepository,
                         ReturnRecordRepository returnRecordRepository,
                         ProductClientService productClientService,
                         ReceiptService receiptService) {
        this.saleRepository = saleRepository;
        this.returnRecordRepository = returnRecordRepository;
        this.productClientService = productClientService;
        this.receiptService = receiptService;
    }

    /**
     * Procesa la devolución total de una venta.
     * 
     * @param saleId ID de la venta
     * @param request datos de la devolución
     * @return respuesta con el recibo de devolución
     * @throws SaleNotFoundException si la venta no existe
     * @throws SaleNotCompletedException si la venta no está COMPLETED
     * @throws SaleAlreadyReturnedException si la venta ya fue devuelta
     */
    @Transactional
    public ReceiptResponse fullReturn(String saleId, ReturnRequest request) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada: " + saleId));
        
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new SaleNotCompletedException("Solo se pueden devolver ventas COMPLETED");
        }
        
        if (sale.getStatus() == SaleStatus.RETURNED) {
            throw new SaleAlreadyReturnedException("La venta ya fue devuelta");
        }
        
        // Incrementar stock de todos los items
        for (SaleItem item : sale.getItems()) {
            productClientService.incrementStock(item.getProductId(), item.getQuantity());
            
            // Crear registro de devolución
            ReturnRecord returnRecord = new ReturnRecord();
            returnRecord.setId(UUID.randomUUID().toString());
            returnRecord.setSaleId(saleId);
            returnRecord.setProductId(item.getProductId());
            returnRecord.setQuantityReturned(item.getQuantity());
            returnRecord.setReturnReason(request.returnReason());
            returnRecord.setReturnedAt(LocalDateTime.now());
            returnRecordRepository.save(returnRecord);
        }
        
        // Cambiar estado a RETURNED
        sale.setStatus(SaleStatus.RETURNED);
        sale.setUpdatedAt(LocalDateTime.now());
        Sale updatedSale = saleRepository.save(sale);
        
        // Generar recibo de devolución
        var receipt = receiptService.generateReturnReceipt(updatedSale, request.returnReason(), "RETURN");
        
        return new ReceiptResponse(
            receipt.getTransactionId(),
            sale.getCreditReferenceNumber(),
            receipt.getReceiptType(),
            null
        );
    }

    /**
     * Procesa la devolución parcial de items específicos de una venta.
     * 
     * @param saleId ID de la venta
     * @param request datos de la devolución parcial
     * @return respuesta con el recibo de devolución parcial
     * @throws SaleNotFoundException si la venta no existe
     * @throws SaleNotCompletedException si la venta no está COMPLETED o PARTIALLY_RETURNED
     */
    @Transactional
    public ReceiptResponse partialReturn(String saleId, PartialReturnRequest request) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada: " + saleId));
        
        if (sale.getStatus() != SaleStatus.COMPLETED && sale.getStatus() != SaleStatus.PARTIALLY_RETURNED) {
            throw new SaleNotCompletedException("Solo se pueden devolver ventas COMPLETED o PARTIALLY_RETURNED");
        }
        
        // Validar que las cantidades devueltas no excedan las compradas
        for (ReturnItemRequest returnItem : request.items()) {
            SaleItem saleItem = sale.getItems().stream()
                .filter(item -> item.getProductId().equals(returnItem.productId()))
                .findFirst()
                .orElseThrow(() -> new SaleNotFoundException(
                    "Producto no encontrado en la venta: " + returnItem.productId()
                ));
            
            // Calcular cantidad ya devuelta
            int alreadyReturned = returnRecordRepository.findBySaleIdAndProductId(saleId, returnItem.productId())
                .stream()
                .mapToInt(ReturnRecord::getQuantityReturned)
                .sum();
            
            int totalReturned = alreadyReturned + returnItem.quantity();
            if (totalReturned > saleItem.getQuantity()) {
                throw new IllegalArgumentException(
                    "Cantidad devuelta excede la cantidad comprada para el producto " + returnItem.productId()
                );
            }
        }
        
        // Procesar devoluciones
        for (ReturnItemRequest returnItem : request.items()) {
            productClientService.incrementStock(returnItem.productId(), returnItem.quantity());
            
            // Crear registro de devolución
            ReturnRecord returnRecord = new ReturnRecord();
            returnRecord.setId(UUID.randomUUID().toString());
            returnRecord.setSaleId(saleId);
            returnRecord.setProductId(returnItem.productId());
            returnRecord.setQuantityReturned(returnItem.quantity());
            returnRecord.setReturnReason(returnItem.returnReason());
            returnRecord.setReturnedAt(LocalDateTime.now());
            returnRecordRepository.save(returnRecord);
        }
        
        // Cambiar estado a PARTIALLY_RETURNED
        sale.setStatus(SaleStatus.PARTIALLY_RETURNED);
        sale.setUpdatedAt(LocalDateTime.now());
        Sale updatedSale = saleRepository.save(sale);
        
        // Generar recibo de devolución parcial
        var receipt = receiptService.generateReturnReceipt(updatedSale, "Devolución parcial", "PARTIAL_RETURN");
        
        return new ReceiptResponse(
            receipt.getTransactionId(),
            sale.getCreditReferenceNumber(),
            receipt.getReceiptType(),
            null
        );
    }
}
