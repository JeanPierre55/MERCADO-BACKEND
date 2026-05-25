package com.mercato.pos.service;

import com.mercato.pos.dto.SaleResponse;
import com.mercato.pos.exception.SaleNotActiveException;
import com.mercato.pos.exception.SaleNotFoundException;
import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleStatus;
import com.mercato.pos.repository.SaleRepository;
import com.mercato.pos.util.MoneyCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar el congelamiento y expiración de ventas.
 */
@Service
public class FreezeService {

    private final SaleRepository saleRepository;
    
    @Value("${pos.frozen-sale-expiration-hours:2}")
    private int frozenSaleExpirationHours;

    public FreezeService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    /**
     * Congela una venta (pausa temporal).
     * 
     * @param saleId ID de la venta
     * @return respuesta con la venta congelada
     * @throws SaleNotFoundException si la venta no existe
     * @throws SaleNotActiveException si la venta no está ACTIVE
     */
    @Transactional
    public SaleResponse freezeSale(String saleId) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada: " + saleId));
        
        if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new SaleNotActiveException("Solo se pueden congelar ventas ACTIVE");
        }
        
        sale.setStatus(SaleStatus.FROZEN);
        sale.setFrozenAt(LocalDateTime.now());
        sale.setUpdatedAt(LocalDateTime.now());
        
        Sale updatedSale = saleRepository.save(sale);
        return toSaleResponse(updatedSale);
    }

    /**
     * Reanuda una venta congelada.
     * 
     * @param saleId ID de la venta
     * @return respuesta con la venta reanudada
     * @throws SaleNotFoundException si la venta no existe
     * @throws SaleNotActiveException si la venta no está FROZEN
     */
    @Transactional
    public SaleResponse resumeSale(String saleId) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada: " + saleId));
        
        if (sale.getStatus() != SaleStatus.FROZEN) {
            throw new SaleNotActiveException("La venta no está congelada");
        }
        
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setFrozenAt(null);
        sale.setUpdatedAt(LocalDateTime.now());
        
        Sale updatedSale = saleRepository.save(sale);
        return toSaleResponse(updatedSale);
    }

    /**
     * Obtiene todas las ventas congeladas de un terminal.
     * 
     * @param terminalId ID del terminal
     * @return lista de ventas congeladas
     */
    public List<SaleResponse> getFrozenSales(String terminalId) {
        List<Sale> frozenSales = saleRepository.findByStatusAndTerminalId(SaleStatus.FROZEN, terminalId);
        return frozenSales.stream()
            .map(this::toSaleResponse)
            .toList();
    }

    /**
     * Expira automáticamente las ventas congeladas que superan el tiempo de expiración.
     * Invocado por el scheduler.
     */
    @Transactional
    public void expireOldFrozenSales() {
        List<Sale> frozenSales = saleRepository.findByStatus(SaleStatus.FROZEN);
        LocalDateTime expirationThreshold = LocalDateTime.now().minusHours(frozenSaleExpirationHours);
        
        for (Sale sale : frozenSales) {
            if (sale.getFrozenAt() != null && sale.getFrozenAt().isBefore(expirationThreshold)) {
                sale.setStatus(SaleStatus.CANCELLED);
                sale.setCancellationReason("Venta congelada expirada");
                sale.setUpdatedAt(LocalDateTime.now());
                saleRepository.save(sale);
            }
        }
    }

    /**
     * Convierte una entidad Sale a SaleResponse.
     */
    private SaleResponse toSaleResponse(Sale sale) {
        var itemDtos = sale.getItems().stream()
            .map(item -> new com.mercato.pos.dto.SaleItemDto(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal()
            ))
            .toList();
        
        return new SaleResponse(
            sale.getId(),
            sale.getTerminalId(),
            sale.getCashierId(),
            sale.getCustomerId(),
            sale.getStatus().toString(),
            itemDtos,
            MoneyCalculator.fromCents(sale.getSubtotalCents()),
            MoneyCalculator.fromCents(sale.getTaxCents()),
            MoneyCalculator.fromCents(sale.getDiscountCents()),
            MoneyCalculator.fromCents(sale.getTotalCents()),
            sale.getPaymentType(),
            sale.getCreatedAt()
        );
    }
}
