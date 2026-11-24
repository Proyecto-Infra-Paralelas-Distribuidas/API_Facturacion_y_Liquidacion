package co.edu.univalle.voltrix.liquidacion.web;

import java.util.regex.Pattern;

/** Validaciones de entrada según el contrato (periodo AAAA-MM, identificadores, rangos). */
final class Validaciones {

    private static final Pattern PERIODO = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");
    private static final Pattern IDENTIFICADOR = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private Validaciones() {
    }

    static void periodo(String campo, String valor) {
        if (valor == null || !PERIODO.matcher(valor).matches()) {
            throw ApiException.solicitudInvalida(campo, "debe tener el formato AAAA-MM");
        }
    }

    static void identificador(String campo, String valor) {
        if (valor == null || !IDENTIFICADOR.matcher(valor).matches()) {
            throw ApiException.solicitudInvalida(campo, "debe ser un identificador válido, por ejemplo PL-001");
        }
    }

    static void rango(String campo, int valor, int minimo, int maximo) {
        if (valor < minimo || valor > maximo) {
            throw ApiException.solicitudInvalida(campo, "debe estar entre " + minimo + " y " + maximo);
        }
    }
}
