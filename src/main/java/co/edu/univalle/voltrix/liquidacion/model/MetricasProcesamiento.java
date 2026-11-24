package co.edu.univalle.voltrix.liquidacion.model;

/** Evidencia de cada ejecución, para el benchmark secuencial contra paralelo. */
public record MetricasProcesamiento(String modo, int workers, int elementos, long duracionMs) {
}
