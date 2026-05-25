package com.mercato.pos.scheduler;

import com.mercato.pos.service.FreezeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler para expirar automáticamente las ventas congeladas.
 */
@Component
public class FrozenSaleExpirationScheduler {

    private final FreezeService freezeService;

    public FrozenSaleExpirationScheduler(FreezeService freezeService) {
        this.freezeService = freezeService;
    }

    /**
     * Ejecuta la verificación de expiración de ventas congeladas de forma periódica.
     * El intervalo se configura con la propiedad pos.frozen-sale-check-interval-ms.
     */
    @Scheduled(fixedDelayString = "${pos.frozen-sale-check-interval-ms:300000}")
    public void checkAndExpireFrozenSales() {
        freezeService.expireOldFrozenSales();
    }
}
