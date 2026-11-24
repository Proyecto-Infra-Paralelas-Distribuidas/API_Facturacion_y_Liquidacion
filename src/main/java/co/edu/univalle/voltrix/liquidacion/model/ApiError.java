package co.edu.univalle.voltrix.liquidacion.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Esquema de error común a todos los equipos (codigo, mensaje, requestId, detalles). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String codigo, String mensaje, String requestId, List<Detalle> detalles) {

    public record Detalle(String campo, String problema) {
    }
}
