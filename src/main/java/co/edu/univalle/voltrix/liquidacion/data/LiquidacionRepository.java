package co.edu.univalle.voltrix.liquidacion.data;

import co.edu.univalle.voltrix.liquidacion.model.Liquidacion;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class LiquidacionRepository {

    private static final String BASE = """
            SELECT id, periodo, planta_id, cliente_id, energia_entregada_kwh, tarifa_por_kwh,
                   valor_energia, costo_mantenimiento, total, moneda, generado_en
            FROM liquidacion
            """;

    private static final RowMapper<Liquidacion> MAPEO = (rs, fila) -> new Liquidacion(
            rs.getString("id"),
            rs.getString("periodo"),
            rs.getString("planta_id"),
            rs.getString("cliente_id"),
            rs.getBigDecimal("energia_entregada_kwh"),
            rs.getBigDecimal("tarifa_por_kwh"),
            rs.getBigDecimal("valor_energia"),
            rs.getBigDecimal("costo_mantenimiento"),
            rs.getBigDecimal("total"),
            rs.getString("moneda"),
            rs.getObject("generado_en", OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    public LiquidacionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Todas las liquidaciones de un periodo, ordenadas por planta. */
    public List<Liquidacion> listarPeriodo(String periodo) {
        return jdbc.query(BASE + " WHERE periodo = ? ORDER BY planta_id", MAPEO, periodo);
    }

    /** Una página de liquidaciones del periodo, opcionalmente de una sola planta. */
    public List<Liquidacion> pagina(String periodo, String plantaId, int limit, int offset) {
        if (plantaId == null) {
            return jdbc.query(BASE + " WHERE periodo = ? ORDER BY planta_id LIMIT ? OFFSET ?",
                    MAPEO, periodo, limit, offset);
        }
        return jdbc.query(BASE + " WHERE periodo = ? AND planta_id = ? ORDER BY planta_id LIMIT ? OFFSET ?",
                MAPEO, periodo, plantaId, limit, offset);
    }

    public int contar(String periodo, String plantaId) {
        Integer n;
        if (plantaId == null) {
            n = jdbc.queryForObject("SELECT COUNT(*) FROM liquidacion WHERE periodo = ?",
                    Integer.class, periodo);
        } else {
            n = jdbc.queryForObject("SELECT COUNT(*) FROM liquidacion WHERE periodo = ? AND planta_id = ?",
                    Integer.class, periodo, plantaId);
        }
        return n == null ? 0 : n;
    }

    /** Suma de los totales de todas las plantas del periodo (no solo de una página). */
    public BigDecimal totalPeriodo(String periodo) {
        BigDecimal suma = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total), 0) FROM liquidacion WHERE periodo = ?",
                BigDecimal.class, periodo);
        return suma == null ? BigDecimal.ZERO : suma;
    }

    /** Comprobación de conectividad para /health. */
    public void ping() {
        jdbc.queryForObject("SELECT 1", Integer.class);
    }
}
