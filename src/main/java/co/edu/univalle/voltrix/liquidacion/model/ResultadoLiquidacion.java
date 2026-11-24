package co.edu.univalle.voltrix.liquidacion.model;

import java.math.BigDecimal;

/** Respuesta de POST /liquidaciones/generar. */
public record ResultadoLiquidacion(
        String periodo,
        int generados,
        BigDecimal totalConsolidado,
        String moneda,
        MetricasProcesamiento metricas) {
}
