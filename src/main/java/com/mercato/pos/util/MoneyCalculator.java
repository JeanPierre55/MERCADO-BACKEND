package com.mercato.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.mercato.pos.model.Sale;

/**
 * Utilidad para cálculos monetarios precisos usando aritmética entera en centavos.
 * 
 * Todos los cálculos intermedios se realizan en centavos (long) para evitar errores
 * de punto flotante. Las conversiones a BigDecimal con 2 decimales se realizan solo
 * al construir respuestas.
 */
public class MoneyCalculator {

    private static final BigDecimal CENTS_PER_DOLLAR = new BigDecimal("100");
    private static final int SCALE = 2;

    /**
     * Convierte un monto en BigDecimal a centavos (long).
     * 
     * @param amount el monto en BigDecimal (ej: 10.50)
     * @return el monto en centavos (ej: 1050)
     * @throws IllegalArgumentException si el monto es negativo
     */
    public static long toCents(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        return amount.multiply(CENTS_PER_DOLLAR).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * Convierte centavos (long) a BigDecimal con escala 2.
     * 
     * @param cents el monto en centavos (ej: 1050)
     * @return el monto en BigDecimal con 2 decimales (ej: 10.50)
     * @throws IllegalArgumentException si los centavos son negativos
     */
    public static BigDecimal fromCents(long cents) {
        if (cents < 0) {
            throw new IllegalArgumentException("Los centavos no pueden ser negativos");
        }
        return new BigDecimal(cents).divide(CENTS_PER_DOLLAR, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Recalcula todos los totales de una Sale basándose en sus items.
     * 
     * Fórmulas:
     * - subtotalCents = Σ(unitPriceCents × quantity) de todos los items
     * - taxCents = round(subtotalCents × taxRate)
     * - totalCents = subtotalCents + taxCents − discountCents
     * 
     * @param sale la venta a recalcular
     * @throws IllegalArgumentException si la venta o sus items tienen valores inválidos
     */
    public static void recalculateTotals(Sale sale) {
        if (sale == null) {
            throw new IllegalArgumentException("La venta no puede ser nula");
        }

        // Calcular subtotal en centavos
        long subtotalCents = 0;
        if (sale.getItems() != null) {
            for (var item : sale.getItems()) {
                if (item.getUnitPriceCents() < 0 || item.getQuantity() < 0) {
                    throw new IllegalArgumentException(
                        "El precio unitario y la cantidad no pueden ser negativos");
                }
                subtotalCents += item.getUnitPriceCents() * item.getQuantity();
            }
        }
        sale.setSubtotalCents(subtotalCents);

        // Calcular tax en centavos
        BigDecimal taxRate = sale.getTaxRate();
        if (taxRate == null) {
            taxRate = new BigDecimal("0.19"); // default 19%
        }
        if (taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("La tasa de impuesto debe estar entre 0 y 1");
        }
        long taxCents = new BigDecimal(subtotalCents)
            .multiply(taxRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();
        sale.setTaxCents(taxCents);

        // Calcular discount en centavos (si no está establecido, es 0)
        long discountCents = sale.getDiscountCents();
        if (discountCents < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo");
        }

        // Calcular total: subtotal + tax - discount
        long totalCents = subtotalCents + taxCents - discountCents;
        if (totalCents < 0) {
            throw new IllegalArgumentException(
                "El total no puede ser negativo (descuento muy alto)");
        }
        sale.setTotalCents(totalCents);
    }
}
