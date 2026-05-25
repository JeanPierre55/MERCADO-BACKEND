package com.mercato.pos.service;

import com.mercato.pos.dto.CheckoutRequest;
import com.mercato.pos.dto.ReceiptResponse;
import com.mercato.pos.exception.CreditNotApprovedException;
import com.mercato.pos.exception.CustomerRequiredException;
import com.mercato.pos.exception.EmptySaleException;
import com.mercato.pos.exception.InsufficientPaymentException;
import com.mercato.pos.exception.InsufficientStockException;
import com.mercato.pos.exception.SaleNotFoundException;
import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleStatus;
import com.mercato.pos.repository.SaleRepository;
import com.mercato.pos.util.MoneyCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio para procesar pagos en efectivo y a crédito.
 */
@Service
public class PaymentService {

    private final SaleRepository saleRepository;
    private final ProductClientService productClientService;
    private final CustomerClientService customerClientService;
    private final ReceiptService receiptService;

    public PaymentService(SaleRepository saleRepository,
                          ProductClientService productClientService,
                          CustomerClientService customerClientService,
                          ReceiptService receiptService) {
        this.saleRepository = saleRepository;
        this.productClientService = productClientService;
        this.customerClientService = customerClientService;
        this.receiptService = receiptService;
    }

    /**
     * Procesa el checkout de una venta (CASH o CREDIT).
     * 
     * @param saleId ID de la venta
     * @param request datos del checkout
     * @return respuesta con el recibo generado
     * @throws SaleNotFoundException si la venta no existe
     * @throws EmptySaleException si la venta no tiene items
     * @throws InsufficientPaymentException si el monto es insuficiente (CASH)
     * @throws CustomerRequiredException si es CREDIT sin cliente
     * @throws CreditNotApprovedException si el crédito no está aprobado
     * @throws InsufficientStockException si no hay stock suficiente
     */
    @Transactional
    public ReceiptResponse checkout(String saleId, CheckoutRequest request) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada: " + saleId));
        
        // Validar que la venta tenga items
        if (sale.getItems().isEmpty()) {
            throw new EmptySaleException("La venta no tiene items");
        }
        
        // Validar stock disponible para todos los items
        for (var item : sale.getItems()) {
            var product = productClientService.getProduct(item.getProductId());
            if (product.availableStock() < item.getQuantity()) {
                throw new InsufficientStockException(
                    "Stock insuficiente para el producto: " + item.getProductId() +
                    ". Disponible: " + product.availableStock() +
                    ", Solicitado: " + item.getQuantity()
                );
            }
        }
        
        if ("CASH".equals(request.paymentType())) {
            return checkoutCash(sale, request);
        } else if ("CREDIT".equals(request.paymentType())) {
            return checkoutCredit(sale, request);
        } else {
            throw new IllegalArgumentException("Tipo de pago no válido: " + request.paymentType());
        }
    }

    /**
     * Procesa el checkout en efectivo.
     */
    private ReceiptResponse checkoutCash(Sale sale, CheckoutRequest request) {
        BigDecimal totalAmount = MoneyCalculator.fromCents(sale.getTotalCents());
        
        // Validar monto recibido
        if (request.amountReceived().compareTo(totalAmount) < 0) {
            throw new InsufficientPaymentException(
                "Monto recibido insuficiente. Total: " + totalAmount +
                ", Recibido: " + request.amountReceived()
            );
        }
        
        // Decrementar stock
        for (var item : sale.getItems()) {
            productClientService.decrementStock(item.getProductId(), item.getQuantity());
        }
        
        // Cambiar estado a COMPLETED
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setPaymentType("CASH");
        sale.setUpdatedAt(LocalDateTime.now());
        
        Sale completedSale = saleRepository.save(sale);
        
        // Generar recibo
        var receipt = receiptService.generateSaleReceipt(completedSale, request.amountReceived());
        
        // Actualizar la venta con el transactionId
        completedSale.setTransactionId(receipt.getTransactionId());
        saleRepository.save(completedSale);
        
        return new ReceiptResponse(
            receipt.getTransactionId(),
            null,
            receipt.getReceiptType(),
            null
        );
    }

    /**
     * Procesa el checkout a crédito.
     */
    private ReceiptResponse checkoutCredit(Sale sale, CheckoutRequest request) {
        // Validar que tenga cliente
        if (sale.getCustomerId() == null || sale.getCustomerId().isBlank()) {
            throw new CustomerRequiredException("Venta a crédito requiere cliente asociado");
        }
        
        // Validar que el cliente tenga crédito aprobado
        var customer = customerClientService.getCustomer(sale.getCustomerId());
        if (!"APPROVED".equals(customer.creditStatus())) {
            throw new CreditNotApprovedException("El cliente no tiene crédito aprobado");
        }
        
        // Generar número de referencia de crédito
        String creditReferenceNumber = "CR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Decrementar stock
        for (var item : sale.getItems()) {
            productClientService.decrementStock(item.getProductId(), item.getQuantity());
        }
        
        // Cambiar estado a COMPLETED
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setPaymentType("CREDIT");
        sale.setCreditReferenceNumber(creditReferenceNumber);
        sale.setUpdatedAt(LocalDateTime.now());
        
        Sale completedSale = saleRepository.save(sale);
        
        // Generar recibo
        var receipt = receiptService.generateSaleReceipt(completedSale, null);
        
        // Actualizar la venta con el transactionId
        completedSale.setTransactionId(receipt.getTransactionId());
        saleRepository.save(completedSale);
        
        return new ReceiptResponse(
            receipt.getTransactionId(),
            creditReferenceNumber,
            receipt.getReceiptType(),
            null
        );
    }
}
