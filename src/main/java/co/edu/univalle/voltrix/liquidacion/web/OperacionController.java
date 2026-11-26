package co.edu.univalle.voltrix.liquidacion.web;

import co.edu.univalle.voltrix.liquidacion.data.LiquidacionRepository;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints comunes a todos los servicios: /, /health y /openapi.yaml. */
@RestController
public class OperacionController {

    private final LiquidacionRepository repositorio;
    private final String version;

    public OperacionController(LiquidacionRepository repositorio, @Value("${app.version}") String version) {
        this.repositorio = repositorio;
        this.version = version;
    }

    @GetMapping("/")
    public Map<String, Object> raiz() {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("servicio", "facturacion-liquidacion");
        cuerpo.put("equipo", "equipo-7");
        cuerpo.put("version", version);
        cuerpo.put("openapi", "/openapi.yaml");
        cuerpo.put("health", "/health");
        cuerpo.put("hora", OffsetDateTime.now(ZoneOffset.UTC).toString());
        return cuerpo;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> salud() {
        boolean baseOk;
        try {
            repositorio.ping();
            baseOk = true;
        } catch (Exception e) {
            baseOk = false;
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", baseOk ? "disponible" : "no_disponible");
        cuerpo.put("baseDatos", baseOk ? "ok" : "error");
        cuerpo.put("version", version);
        cuerpo.put("tiempoActivoSeg", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        return ResponseEntity.status(baseOk ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(cuerpo);
    }

    @GetMapping(value = "/openapi.yaml", produces = "application/yaml")
    public ResponseEntity<String> contrato() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/openapi.yaml")) {
            if (in == null) {
                throw ApiException.noEncontrado("El contrato no está empaquetado en la imagen.");
            }
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/yaml"))
                    .body(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}