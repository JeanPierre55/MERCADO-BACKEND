package com.mercato.pos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mercato.pos.model.Receipt;

/**
 * Repositorio Spring Data JPA para la entidad Receipt.
 */
@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, String> {

    /**
     * Busca un recibo por su Transaction_ID único.
     * 
     * @param transactionId el ID de transacción
     * @return Optional con el recibo si existe, vacío en caso contrario
     */
    Optional<Receipt> findByTransactionId(String transactionId);
}
