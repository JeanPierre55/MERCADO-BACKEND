package com.mercato.pos.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleStatus;

/**
 * Tests de integración para SaleRepository.
 */
@DataJpaTest
class SaleRepositoryTest {

    @Autowired
    private SaleRepository saleRepository;

    private Sale sale1;
    private Sale sale2;
    private Sale sale3;

    @BeforeEach
    void setUp() {
        // Crear ventas de prueba
        sale1 = new Sale("TERM001", "CASH001");
        sale1.setStatus(SaleStatus.ACTIVE);

        sale2 = new Sale("TERM001", "CASH001");
        sale2.setStatus(SaleStatus.FROZEN);

        sale3 = new Sale("TERM002", "CASH002");
        sale3.setStatus(SaleStatus.ACTIVE);

        saleRepository.save(sale1);
        saleRepository.save(sale2);
        saleRepository.save(sale3);
    }

    @Test
    void testFindByStatusAndTerminalId() {
        // Act
        List<Sale> frozenSales = saleRepository.findByStatusAndTerminalId(SaleStatus.FROZEN, "TERM001");

        // Assert
        assertEquals(1, frozenSales.size());
        assertEquals(sale2.getId(), frozenSales.get(0).getId());
    }

    @Test
    void testFindByStatusAndTerminalId_MultipleSales() {
        // Act
        List<Sale> activeSales = saleRepository.findByStatusAndTerminalId(SaleStatus.ACTIVE, "TERM001");

        // Assert
        assertEquals(1, activeSales.size());
        assertEquals(sale1.getId(), activeSales.get(0).getId());
    }

    @Test
    void testFindByStatusAndTerminalId_NoResults() {
        // Act
        List<Sale> results = saleRepository.findByStatusAndTerminalId(SaleStatus.COMPLETED, "TERM001");

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindByIdAndStatus() {
        // Act
        Optional<Sale> result = saleRepository.findByIdAndStatus(sale1.getId(), SaleStatus.ACTIVE);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(sale1.getId(), result.get().getId());
    }

    @Test
    void testFindByIdAndStatus_WrongStatus() {
        // Act
        Optional<Sale> result = saleRepository.findByIdAndStatus(sale1.getId(), SaleStatus.FROZEN);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByIdAndStatus_NotFound() {
        // Act
        Optional<Sale> result = saleRepository.findByIdAndStatus("NONEXISTENT", SaleStatus.ACTIVE);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testSaveSale() {
        // Arrange
        Sale newSale = new Sale("TERM003", "CASH003");

        // Act
        Sale saved = saleRepository.save(newSale);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("TERM003", saved.getTerminalId());
    }

    @Test
    void testUpdateSale() {
        // Arrange
        sale1.setStatus(SaleStatus.COMPLETED);

        // Act
        Sale updated = saleRepository.save(sale1);

        // Assert
        assertEquals(SaleStatus.COMPLETED, updated.getStatus());
    }
}
