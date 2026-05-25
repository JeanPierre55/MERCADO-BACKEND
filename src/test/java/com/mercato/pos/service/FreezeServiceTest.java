package com.mercato.pos.service;

import com.mercato.pos.dto.SaleResponse;
import com.mercato.pos.exception.SaleNotActiveException;
import com.mercato.pos.exception.SaleNotFoundException;
import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleStatus;
import com.mercato.pos.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FreezeServiceTest {

    private FreezeService freezeService;

    @Mock
    private SaleRepository saleRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        freezeService = new FreezeService(saleRepository);
    }

    @Test
    void testFreezeSaleSuccess() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setCreatedAt(LocalDateTime.now());
        sale.setUpdatedAt(LocalDateTime.now());

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        // Act
        SaleResponse response = freezeService.freezeSale("SALE001");

        // Assert
        assertNotNull(response);
        assertEquals("SALE001", response.id());
        verify(saleRepository, times(1)).findById("SALE001");
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    void testFreezeSaleNotFound() {
        // Arrange
        when(saleRepository.findById("SALE001")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SaleNotFoundException.class, () -> freezeService.freezeSale("SALE001"));
    }

    @Test
    void testFreezeSaleNotActive() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.COMPLETED);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        // Act & Assert
        assertThrows(SaleNotActiveException.class, () -> freezeService.freezeSale("SALE001"));
    }

    @Test
    void testResumeSaleSuccess() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.FROZEN);
        sale.setFrozenAt(LocalDateTime.now());

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        // Act
        SaleResponse response = freezeService.resumeSale("SALE001");

        // Assert
        assertNotNull(response);
        assertEquals("SALE001", response.id());
        verify(saleRepository, times(1)).findById("SALE001");
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    void testResumeSaleNotFrozen() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.ACTIVE);

        when(saleRepository.findById("SALE001")).thenReturn(Optional.of(sale));

        // Act & Assert
        assertThrows(SaleNotActiveException.class, () -> freezeService.resumeSale("SALE001"));
    }

    @Test
    void testGetFrozenSales() {
        // Arrange
        Sale sale1 = new Sale("TERM001", "CASHIER001");
        sale1.setId("SALE001");
        sale1.setStatus(SaleStatus.FROZEN);

        Sale sale2 = new Sale("TERM001", "CASHIER001");
        sale2.setId("SALE002");
        sale2.setStatus(SaleStatus.FROZEN);

        when(saleRepository.findByStatusAndTerminalId(SaleStatus.FROZEN, "TERM001"))
            .thenReturn(List.of(sale1, sale2));

        // Act
        List<SaleResponse> response = freezeService.getFrozenSales("TERM001");

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
        verify(saleRepository, times(1)).findByStatusAndTerminalId(SaleStatus.FROZEN, "TERM001");
    }

    @Test
    void testExpireOldFrozenSales() {
        // Arrange
        Sale sale = new Sale("TERM001", "CASHIER001");
        sale.setId("SALE001");
        sale.setStatus(SaleStatus.FROZEN);
        sale.setFrozenAt(LocalDateTime.now().minusHours(3));

        when(saleRepository.findByStatus(SaleStatus.FROZEN)).thenReturn(List.of(sale));
        when(saleRepository.save(any(Sale.class))).thenReturn(sale);

        // Act
        freezeService.expireOldFrozenSales();

        // Assert
        verify(saleRepository, times(1)).findByStatus(SaleStatus.FROZEN);
        verify(saleRepository, times(1)).save(any(Sale.class));
    }
}
