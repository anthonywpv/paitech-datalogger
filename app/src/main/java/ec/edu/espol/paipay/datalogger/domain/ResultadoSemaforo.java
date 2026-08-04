package ec.edu.espol.paipay.datalogger.domain;

import java.util.ArrayList;
import java.util.List;

/** Evaluación completa de una piscina: estado global + detalle por parámetro. */
public class ResultadoSemaforo {

    private final String piscina;
    private final String fecha;
    private final List<LecturaEvaluada> lecturas = new ArrayList<>();

    public ResultadoSemaforo(String piscina, String fecha) {
        this.piscina = piscina;
        this.fecha = fecha;
    }

    public void agregar(LecturaEvaluada lectura) {
        lecturas.add(lectura);
    }

    public String getPiscina() { return piscina; }

    public String getFecha() { return fecha; }

    public List<LecturaEvaluada> getLecturas() { return lecturas; }

    /** El estado global es el peor de todos los parámetros evaluados. */
    public EstadoAlerta getEstadoGlobal() {
        EstadoAlerta global = EstadoAlerta.SIN_DATO;
        for (LecturaEvaluada l : lecturas) {
            global = EstadoAlerta.peor(global, l.estado);
        }
        return global;
    }

    /** Mensaje corto para el encabezado de la tarjeta. */
    public String resumen() {
        switch (getEstadoGlobal()) {
            case VERDE:
                return "Condiciones adecuadas para la Vieja Azul.";
            case AMARILLO:
                return "Hay parámetros fuera del rango ideal. Vigilar de cerca.";
            case ROJO:
                return "Riesgo alto para los peces. Requiere acción inmediata.";
            default:
                return "Sin mediciones registradas.";
        }
    }

    /** Primera acción concreta sugerida (la del parámetro más grave). */
    public String accionPrioritaria() {
        LecturaEvaluada peor = null;
        for (LecturaEvaluada l : lecturas) {
            if (peor == null || l.estado.ordinal() > peor.estado.ordinal()) peor = l;
        }
        return peor == null ? "" : peor.recomendacion;
    }
}
