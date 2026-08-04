package ec.edu.espol.paipay.datalogger.domain;

/** Resultado de evaluar un parámetro individual (pH, amonio, nitrito, nitrato…). */
public class LecturaEvaluada {

    public final String parametro;
    public final double valor;
    public final String unidad;
    public final EstadoAlerta estado;
    public final String diagnostico;
    public final String recomendacion;

    public LecturaEvaluada(String parametro, double valor, String unidad,
                           EstadoAlerta estado, String diagnostico, String recomendacion) {
        this.parametro = parametro;
        this.valor = valor;
        this.unidad = unidad;
        this.estado = estado;
        this.diagnostico = diagnostico;
        this.recomendacion = recomendacion;
    }

    public String valorFormateado() {
        return String.format(java.util.Locale.US, "%.1f %s", valor, unidad);
    }
}
