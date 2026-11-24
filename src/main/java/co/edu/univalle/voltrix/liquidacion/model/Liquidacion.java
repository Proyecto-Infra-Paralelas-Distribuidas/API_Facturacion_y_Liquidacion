package co.edu.univalle.voltrix.liquidacion.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Liquidación de una planta en un periodo (esquema Liquidacion del contrato). */
public record Liquidacion(
        String id,
        String periodo,
        String plantaId,
        String clienteId,
        BigDecimal energiaEntregadaKwh,
        BigDecimal tarifaPorKwh,
        BigDecimal valorEnergia,
        BigDecimal costoMantenimiento,
        BigDecimal total,
        String moneda,
        OffsetDateTime generadoEn) {
}
