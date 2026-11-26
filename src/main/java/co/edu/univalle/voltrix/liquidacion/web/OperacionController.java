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

/** Endpoints comunes a todos los servicios: /, /docs, /health y /openapi.yaml. */
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
        cuerpo.put("docs", "/docs");
        cuerpo.put("openapi", "/openapi.yaml");
        cuerpo.put("health", "/health");
        cuerpo.put("hora", OffsetDateTime.now(ZoneOffset.UTC).toString());
        return cuerpo;
    }

    private static final String PAGINA_DOCS = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <title>API Facturación y Liquidación - Equipo 7</title>
              <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
            </head>
            <body>
              <div id="swagger-ui"></div>
              <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
              <script>
                window.ui = SwaggerUIBundle({
                  url: "/openapi.yaml",
                  dom_id: "#swagger-ui",
                  requestInterceptor: function (req) {
                    try {
                      var u = new URL(req.url);
                      req.url = window.location.origin + u.pathname + u.search;
                    } catch (e) { }
                    return req;
                  },
                  onComplete: function () {
                    var spec = window.ui.specSelectors.specJson().toJS();
                    spec.servers = [{ url: window.location.origin, description: "Servidor actual" }];
                    window.ui.specActions.updateJsonSpec(spec);
                  }
                });
              </script>
            </body>
            </html>
            """;

    @GetMapping(value = "/docs", produces = "text/html;charset=UTF-8")
    public String documentacion() {
        return PAGINA_DOCS;
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