package ec.edu.espol.paipay.datalogger.domain;

/** Reglas numéricas compartidas por la captura manual de calidad del agua. */
public final class ValidadorAgua {
    private ValidadorAgua() { }

    public static boolean esDecimalValido(String texto, int decimales, double maximo) {
        if (texto == null || texto.isEmpty()) return true;
        String normalizado = texto.replace(',', '.');
        if (!normalizado.matches("\\d+(\\.\\d+)?")) return false;
        int separador = normalizado.indexOf('.');
        int cantidadDecimales = separador < 0 ? 0 : normalizado.length() - separador - 1;
        if (cantidadDecimales > decimales) return false;
        try {
            double valor = Double.parseDouble(normalizado);
            return Double.isFinite(valor) && valor >= 0 && valor <= maximo;
        } catch (NumberFormatException error) {
            return false;
        }
    }
}
