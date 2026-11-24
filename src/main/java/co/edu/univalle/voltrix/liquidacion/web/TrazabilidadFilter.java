package co.edu.univalle.voltrix.liquidacion.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Asigna X-Request-Id a cada respuesta y deja en el log una línea JSON por petición,
 * con el equipo consumidor (X-Equipo-Consumidor). Es la evidencia de consumo real entre células.
 */
@Component
public class TrazabilidadFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_REQUEST_ID = "requestId";

    private static final Logger LOG = LoggerFactory.getLogger("peticiones");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        req.setAttribute(ATRIBUTO_REQUEST_ID, requestId);
        res.setHeader("X-Request-Id", requestId);
        long inicio = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000;
            LOG.info("{\"evento\":\"peticion\",\"requestId\":\"{}\",\"metodo\":\"{}\",\"ruta\":\"{}\","
                            + "\"estado\":{},\"equipoConsumidor\":\"{}\",\"duracionMs\":{}}",
                    requestId, req.getMethod(), limpiar(req.getRequestURI()), res.getStatus(),
                    limpiar(req.getHeader("X-Equipo-Consumidor")), duracionMs);
        }
    }

    /** Evita que un valor del cliente rompa el formato JSON del log. */
    private static String limpiar(String valor) {
        return valor == null ? "" : valor.replaceAll("[^A-Za-z0-9_./{}-]", "");
    }
}
