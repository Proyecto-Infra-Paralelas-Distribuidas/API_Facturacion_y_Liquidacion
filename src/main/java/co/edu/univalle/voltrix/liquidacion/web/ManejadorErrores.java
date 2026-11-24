package co.edu.univalle.voltrix.liquidacion.web;

import co.edu.univalle.voltrix.liquidacion.model.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduce cualquier excepción al esquema de error común del contrato. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> negocio(ApiException e, HttpServletRequest req) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(e.estado());
        if (e.reintentarEnSeg() != null) {
            respuesta.header("Retry-After", String.valueOf(e.reintentarEnSeg()));
        }
        return respuesta.body(new ApiError(e.codigo(), e.getMessage(), requestId(req), e.detalles()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> baseDatos(DataAccessException e, HttpServletRequest req) {
        LOG.error("Falla de base de datos", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                new ApiError("NO_DISPONIBLE", "No hay conexión con la base de datos.", requestId(req), null));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> solicitudMalformada(Exception e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(
                "SOLICITUD_INVALIDA", "La solicitud no cumple el contrato.", requestId(req),
                List.of(new ApiError.Detalle("solicitud", "cuerpo o parámetro con formato inválido"))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> otro(Exception e, HttpServletRequest req) {
        if (e instanceof ErrorResponse er) {
            int estado = er.getStatusCode().value();
            String codigo = estado == 404 ? "NO_ENCONTRADO"
                    : estado == 405 ? "METODO_NO_PERMITIDO" : "SOLICITUD_INVALIDA";
            return ResponseEntity.status(estado).body(
                    new ApiError(codigo, "La solicitud no pudo atenderse.", requestId(req), null));
        }
        LOG.error("Error interno", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiError("ERROR_INTERNO", "Error interno del servicio.", requestId(req), null));
    }

    private static String requestId(HttpServletRequest req) {
        Object id = req.getAttribute(TrazabilidadFilter.ATRIBUTO_REQUEST_ID);
        return id == null ? "sin-id" : id.toString();
    }
}
