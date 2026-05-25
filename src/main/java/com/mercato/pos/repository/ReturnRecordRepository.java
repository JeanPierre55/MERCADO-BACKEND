package com.mercato.pos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mercato.pos.model.ReturnRecord;

/**
 * Repositorio Spring Data JPA para la entidad ReturnRecord.
 */
@Repository
public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, String> {

    /**
     * Busca todos los registros de devolución para un producto específico en una venta.
     * 
     * @param saleId el ID de la venta
     * @param productId el ID del producto
     * @return lista de registros de devolución que coinciden
     */
    List<ReturnRecord> findBySaleIdAndProductId(String saleId, String productId);

    /**
     * Busca todos los registros de devolución para una venta específica.
     * 
     * @param saleId el ID de la venta
     * @return lista de registros de devolución de la venta
     */
    List<ReturnRecord> findBySaleId(String saleId);
}
