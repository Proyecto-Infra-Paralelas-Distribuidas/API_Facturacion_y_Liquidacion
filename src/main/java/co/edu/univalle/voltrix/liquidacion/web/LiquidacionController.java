package co.edu.univalle.voltrix.liquidacion.web;

import co.edu.univalle.voltrix.liquidacion.data.LiquidacionRepository;
import co.edu.univalle.voltrix.liquidacion.model.Liquidacion;
import co.edu.univalle.voltrix.liquidacion.model.MetricasProcesamiento;
import co.edu.univalle.voltrix.liquidacion.model.PaginaLiquidaciones;
import co.edu.univalle.voltrix.liquidacion.model.ResultadoLiquidacion;
import co.edu.univalle.voltrix.liquidacion.model.SolicitudLiquidacion;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avance 1 (esqueleto): responde con los datos de ejemplo guardados en la base de datos.
 * Todavía NO consulta a Reportes de Generación ni a Mantenimiento Predictivo y NO calcula;
 * eso se implementa en el Avance 2.
 */
@RestController
public class LiquidacionController {

    private static final String MONEDA = "COP";

    private final LiquidacionRepository repositorio;
    private final Semaphore cupos;

    public LiquidacionController(LiquidacionRepository repositorio,
                                 @Value("${app.max-concurrent:2}") int maxConcurrentes) {
        this.repositorio = repositorio;
        this.cupos = new Semaphore(Math.max(1, maxConcurrentes));
    }

    @PostMapping("/liquidaciones/generar")
    public ResultadoLiquidacion generar(
            @RequestBody SolicitudLiquidacion solicitud,
            @RequestParam(name = "modo", defaultValue = "paralelo") String modo,
            @RequestParam(name = "workers", required = false) Integer workers) {

        if (solicitud == null) {
            throw ApiException.solicitudInvalida("solicitud", "el cuerpo es obligatorio");
        }
        Validaciones.periodo("periodo", solicitud.periodo());
        if (solicitud.plantaIds() != null) {
            for (String id : solicitud.plantaIds()) {
                Validaciones.identificador("plantaIds", id);
            }
        }
        if (!modo.equals("secuencial") && !modo.equals("paralelo")) {
            throw ApiException.solicitudInvalida("modo", "debe ser secuencial o paralelo");
        }
        if (workers != null) {
            Validaciones.rango("workers", workers, 1, 64);
        }

        // Control de carga: si ya hay MAX_CONCURRENT_JOBS cálculos en curso, se rechaza con 429.
        if (!cupos.tryAcquire()) {
            throw ApiException.sobrecarga();
        }
        try {
            long inicio = System.nanoTime();
            List<Liquidacion> filas = repositorio.listarPeriodo(solicitud.periodo());
            if (solicitud.plantaIds() != null && !solicitud.plantaIds().isEmpty()) {
                filas = filas.stream().filter(l -> solicitud.plantaIds().contains(l.plantaId())).toList();
            }
            BigDecimal total = filas.stream().map(Liquidacion::total)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int hilos = modo.equals("secuencial") ? 1
                    : (workers != null ? workers : Runtime.getRuntime().availableProcessors());
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000;
            return new ResultadoLiquidacion(solicitud.periodo(), filas.size(), total, MONEDA,
                    new MetricasProcesamiento(modo, hilos, filas.size(), duracionMs));
        } finally {
            cupos.release();
        }
    }

    @GetMapping("/liquidaciones/{periodo}")
    public PaginaLiquidaciones consultar(
            @PathVariable("periodo") String periodo,
            @RequestParam(name = "plantaId", required = false) String plantaId,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset) {

        Validaciones.periodo("periodo", periodo);
        if (plantaId != null) {
            Validaciones.identificador("plantaId", plantaId);
        }
        Validaciones.rango("limit", limit, 1, 200);
        if (offset < 0) {
            throw ApiException.solicitudInvalida("offset", "debe ser mayor o igual a 0");
        }

        int total = repositorio.contar(periodo, plantaId);
        if (total == 0) {
            throw ApiException.noEncontrado("No hay liquidaciones generadas para el periodo " + periodo + ".");
        }
        List<Liquidacion> datos = repositorio.pagina(periodo, plantaId, limit, offset);
        return new PaginaLiquidaciones(periodo, MONEDA, repositorio.totalPeriodo(periodo),
                datos, total, limit, offset);
    }
}
