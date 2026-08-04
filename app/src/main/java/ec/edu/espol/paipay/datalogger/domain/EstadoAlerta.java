package ec.edu.espol.paipay.datalogger.domain;

/**
 * Los tres estados del Semáforo de Alertas, más un cuarto para ausencia de dato.
 * El orden del enum define la severidad: a mayor ordinal, más grave.
 */
public enum EstadoAlerta {
    SIN_DATO,
    VERDE,
    AMARILLO,
    ROJO;

    /** Devuelve el más grave de dos estados. Un solo parámetro en rojo pone la piscina en rojo. */
    public static EstadoAlerta peor(EstadoAlerta a, EstadoAlerta b) {
        if (a == SIN_DATO) return b;
        if (b == SIN_DATO) return a;
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
