package com.mercato.pos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mercato.pos.model.Sale;
import com.mercato.pos.model.SaleStatus;

/**
 * Repositorio Spring Data JPA para la entidad Sale.
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, String> {

    /**
     * Busca todas las ventas con un estado específico en un terminal.
     * 
     * @param status el estado de la venta
     * @param terminalId el ID del terminal
     * @return lista de ventas que coinciden con los criterios
     */
    List<Sale> findByStatusAndTerminalId(SaleStatus status, String terminalId);

    /**
     * Busca una venta por ID y estado específico.
     * 
     * @param id el ID de la venta
     * @param status el estado de la venta
     * @return Optional con la venta si existe, vacío en caso contrario
     */
    Optional<Sale> findByIdAndStatus(String id, SaleStatus status);

    /**
     * Busca todas las ventas con un estado específico.
     * 
     * @param status el estado de la venta
     * @return lista de ventas que coinciden con el estado
     */
    List<Sale> findByStatus(SaleStatus status);
}
