package com.mercato.pos.service;

import com.mercato.pos.dto.AddItemRequest;
import com.mercato.pos.dto.CancelSaleRequest;
import com.mercato.pos.dto.CreateSaleRequest;
import com.mercato.pos.dto.SaleItemDto;
import com.mercato.pos.dto.SaleResponse;
import com.mercato.pos.dto.UpdateItemRequest;
import com.mercato.pos.exception.InsufficientStockException;
import com.mercato.pos.exception.InvalidQuantityException;
import com.mercato.pos.exception.SaleNotActiveException;
import com.mercato.pos.exception.SaleNotFoundException;
import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleItem;
import com.mercato.pos.model.SaleStatus;
import com.mercato.pos.repository.SaleItemRepository;
import com.mercato.pos.repository.SaleRepository;
import com.mercato.pos.util.MoneyCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para gestionar el ciclo de vida de las ventas.
 */
@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductClientService productClientService;

    public SaleService(SaleRepository saleRepository,
                       SaleItemRepository saleItemRepository,
                       ProductClientService productClientService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productClientService = productClientService;
    }

    /**
     * Crea una nueva venta.
     * 
     * @param request datos de la solicitud de creación
     * @param cashierId ID del cajero (extraído del JWT)
     * @return respuesta con la venta creada
     */
    @Transactional
    public SaleResponse createSale(CreateSaleRequest request, String cashierId) {
        Sale sale = new Sale(request.terminalId(), cashierId);
        sale.setId(UUID.randomUUID().toString());
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setSubtotalCents(0);
        sale.setTaxCents(0);
        sale.setDiscountCents(0);
        sale.setTotalCents(0);
        
        if (request.customerId() != null && !request.customerId().isBlank()) {
            sale.setCustomerId(request.customerId());
        }
        
        sale.setCreatedAt(LocalDateTime.now());
        sale.setUpdatedAt(LocalDateTime.now());
        
        Sale savedSale = saleRepository.save(sale);
        return toSaleResponse(savedSale);
    }

    /**
     * Obtiene una venta por su ID.
     * 
     * @param saleId ID de la venta
     * @return respuesta con la venta
     * @throws SaleNotFoundException si la venta no existe
     */
    public SaleResponse getSale(String saleId) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada: " + saleId));
        return toSaleResponse(sale);
    }

    /**
     * Agrega un item a una venta.
     * 
     * @param saleId ID de la venta
     * @param request datos del item a agregar
     * @return respuesta con la venta actualizada
     * @throws SaleNotFoundException si la venta no existe
     * @throws SaleNotActiveException si la venta no está ACTIVE
     * @throws InvalidQuantityException si la cantidad es inválida
     * @throws InsufficientStockException si no hay stock suficiente
     */
    @Transactional
    public SaleResponse addItem(String saleId, AddItemRequest request) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada: " + saleId));
        
        if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new SaleNotActiveException("La venta no está activa");
        }
        
        if (request.quantity() < 1) {
            throw new InvalidQuantityException("La cantidad debe ser mayor o igual a 1");
        }
        
        // Obtener el producto de la Product API
        var product = productClientService.getProduct(request.productId());
        
        // Verificar stock disponible
        int totalQuantityForProduct = request.quantity();
        for (SaleItem item : sale.getItems()) {
            if (item.getProductId().equals(request.productId())) {
                totalQuantityForProduct += item.getQuantity();
            }
        }
        
        if (product.availableStock() < totalQuantityForProduct) {
            throw new InsufficientStockException(
                "Stock insuficiente para el producto: " + request.productId() +
                ". Disponible: " + product.availableStock() +
                ", Solicitado: " + totalQuantityForProduct
            );
        }
        
        // Buscar si el producto ya existe en la venta
        SaleItem existingItem = sale.getItems().stream()
            .filter(item -> item.getProductId().equals(request.productId()))
            .findFirst()
            .orElse(null);
        
        if (existingItem != null) {
            // Incrementar cantidad del item existente
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
            long newLineTotalCents = existingItem.getUnitPriceCents() * existingItem.getQuantity();
            existingItem.setLineTotalCents(newLineTotalCents);
            existingItem.setLineTotal(MoneyCalculator.fromCents(newLineTotalCents));
        } else {
            // Crear nuevo item con snapshot de precio
            long unitPriceCents = MoneyCalculator.toCents(product.unitPrice());
            long lineTotalCents = unitPriceCents * request.quantity();
            
            SaleItem newItem = new SaleItem(
                request.productId(),
                product.name(),
                request.barcode(),
                product.unitPrice(),
                unitPriceCents,
                request.quantity()
            );
            newItem.setId(UUID.randomUUID().toString());
            newItem.setLineTotalCents(lineTotalCents);
            newItem.setLineTotal(MoneyCalculator.fromCents(lineTotalCents));
            newItem.setSale(sale);
            
            sale.getItems().add(newItem);
        }
        
        // Recalcular totales
        MoneyCalculator.recalculateTotals(sale);
        sale.setUpdatedAt(LocalDateTime.now());
        
        Sale updatedSale = saleRepository.save(sale);
        return toSaleResponse(updatedSale);
    }

    /**
     * Actualiza la cantidad de un item en una venta.
     * 
     * @param saleId ID de la venta
     * @param itemId ID del item
     * @param request datos de actualización
     * @return respuesta con la venta actualizada
     * @throws SaleNotFoundException si la venta no existe
     * @throws SaleNotActiveException si la venta no está ACTIVE
     * @throws InvalidQuantityException si la cantidad es inválida
     * @throws InsufficientStockException si no hay stock suficiente
     */
    @Transactional
    public SaleResponse updateItem(String saleId, String itemId, UpdateItemRequest request) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada: " + saleId));
        
        if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new SaleNotActiveException("La venta no está activa");
        }
        
        if (request.quantity() < 1) {
            throw new InvalidQuantityException("La cantidad debe ser mayor o igual a 1");
        }
        
        SaleItem item = sale.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new SaleNotFoundException("Item no encontrado: " + itemId));
        
        // Verificar stock disponible
        int quantityDifference = request.quantity() - item.getQuantity();
        int totalQuantityForProduct = 0;
        for (SaleItem i : sale.getItems()) {
            if (i.getProductId().equals(item.getProductId())) {
                totalQuantityForProduct += i.getQuantity();
            }
        }
        totalQuantityForProduct += quantityDifference;
        
        var product = productClientService.getProduct(item.getProductId());
        if (product.availableStock() < totalQuantityForProduct) {
            throw new InsufficientStockException(
                "Stock insuficiente para el producto: " + item.getProductId() +
                ". Disponible: " + product.availableStock() +
                ", Solicitado: " + totalQuantityForProduct
            );
        }
        
        // Actualizar cantidad y recalcular total de línea
        item.setQuantity(request.quantity());
        long newLineTotalCents = item.getUnitPriceCents() * request.quantity();
        item.setLineTotalCents(newLineTotalCents);
        item.setLineTotal(MoneyCalculator.fromCents(newLineTotalCents));
        
        // Recalcular totales
        MoneyCalculator.recalculateTotals(sale);
        sale.setUpdatedAt(LocalDateTime.now());
        
        Sale updatedSale = saleRepository.save(sale);
        return toSaleResponse(updatedSale);
    }

    /**
     * Elimina un item de una venta.
     * 
     * @param saleId ID de la venta
     * @param itemId ID del item
     * @return respuesta con la venta actualizada
     * @throws SaleNotFoundException si la venta o el item no existen
     * @throws SaleNotActiveException si la venta no está ACTIVE
     */
    @Transactional
    public SaleResponse removeItem(String saleId, String itemId) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada: " + saleId));
        
        if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new SaleNotActiveException("La venta no está activa");
        }
        
        SaleItem item = sale.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new SaleNotFoundException("Item no encontrado: " + itemId));
        
        sale.getItems().remove(item);
        
        // Recalcular totales
        MoneyCalculator.recalculateTotals(sale);
        sale.setUpdatedAt(LocalDateTime.now());
        
        Sale updatedSale = saleRepository.save(sale);
        return toSaleResponse(updatedSale);
    }

    /**
     * Cancela una venta.
     * 
     * @param saleId ID de la venta
     * @param request datos de cancelación
     * @return respuesta con la venta cancelada
     * @throws SaleNotFoundException si la venta no existe
     * @throws SaleNotActiveException si la venta no está ACTIVE o FROZEN
     */
    @Transactional
    public SaleResponse cancelSale(String saleId, CancelSaleRequest request) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new SaleNotFoundException("Venta no encontrada: " + saleId));
        
        if (sale.getStatus() != SaleStatus.ACTIVE && sale.getStatus() != SaleStatus.FROZEN) {
            throw new SaleNotActiveException("Solo se pueden cancelar ventas ACTIVE o FROZEN");
        }
        
        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancellationReason(request.cancellationReason());
        sale.setUpdatedAt(LocalDateTime.now());
        
        Sale updatedSale = saleRepository.save(sale);
        return toSaleResponse(updatedSale);
    }

    /**
     * Convierte una entidad Sale a SaleResponse.
     */
    private SaleResponse toSaleResponse(Sale sale) {
        List<SaleItemDto> itemDtos = sale.getItems().stream()
            .map(item -> new SaleItemDto(
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
