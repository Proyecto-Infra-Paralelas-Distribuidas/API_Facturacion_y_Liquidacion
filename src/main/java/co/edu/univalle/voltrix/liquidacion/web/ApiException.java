package co.edu.univalle.voltrix.liquidacion.web;

import co.edu.univalle.voltrix.liquidacion.model.ApiError;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Error de negocio o de validación que se traduce al esquema de error común. */
public class ApiException extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;
    private final List<ApiError.Detalle> detalles;
    private final Integer reintentarEnSeg;

    public ApiException(HttpStatus estado, String codigo, String mensaje,
                        List<ApiError.Detalle> detalles, Integer reintentarEnSeg) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
        this.detalles = detalles;
        this.reintentarEnSeg = reintentarEnSeg;
    }

    public static ApiException solicitudInvalida(String campo, String problema) {
        return new ApiException(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud no cumple el contrato.",
                List.of(new ApiError.Detalle(campo, problema)), null);
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje, null, null);
    }

    public static ApiException sobrecarga() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "SOBRECARGA",
                "El servicio está atendiendo el máximo de peticiones simultáneas.", null, 1);
    }

    public HttpStatus estado() {
        return estado;
    }

    public String codigo() {
        return codigo;
    }

    public List<ApiError.Detalle> detalles() {
        return detalles;
    }

    public Integer reintentarEnSeg() {
        return reintentarEnSeg;
    }
}
