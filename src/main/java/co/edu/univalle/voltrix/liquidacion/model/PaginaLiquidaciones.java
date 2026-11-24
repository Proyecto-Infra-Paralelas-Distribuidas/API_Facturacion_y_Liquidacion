package co.edu.univalle.voltrix.liquidacion.model;

import java.math.BigDecimal;
import java.util.List;

/** Respuesta de GET /liquidaciones/{periodo}. */
public record PaginaLiquidaciones(
        String periodo,
        String moneda,
        BigDecimal totalConsolidado,
        List<Liquidacion> datos,
        int total,
        int limit,
        int offset) {
}
