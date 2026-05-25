package com.mercato.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mercato.pos.model.SaleItem;

/**
 * Repositorio Spring Data JPA para la entidad SaleItem.
 */
@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, String> {
}
