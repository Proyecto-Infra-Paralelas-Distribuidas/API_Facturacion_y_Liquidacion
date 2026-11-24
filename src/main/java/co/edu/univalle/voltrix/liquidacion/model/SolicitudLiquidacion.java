package co.edu.univalle.voltrix.liquidacion.model;

import java.util.List;

/** Cuerpo de POST /liquidaciones/generar. plantaIds es opcional. */
public record SolicitudLiquidacion(String periodo, List<String> plantaIds) {
}
