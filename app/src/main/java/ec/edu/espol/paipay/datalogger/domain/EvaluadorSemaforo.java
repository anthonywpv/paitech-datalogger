package ec.edu.espol.paipay.datalogger.domain;

/**
 * ===========================================================================
 *  SEMÁFORO DE ALERTAS — LÓGICA DE NEGOCIO (BACKEND EN JAVA)
 * ===========================================================================
 *
 * Evalúa la calidad del agua de una piscina y devuelve VERDE, AMARILLO o ROJO
 * con un diagnóstico y una recomendación en lenguaje sencillo, pensado para
 * productores con baja alfabetización digital.
 *
 * Especie objetivo: Vieja Azul (Andinoacara rivulatus), cíclido nativo de la
 * cuenca del Guayas.
 *
 * EL CICLO DEL NITRÓGENO, que es de lo que trata este semáforo:
 *
 *     peces → AMONÍACO TOTAL (NH3/NH4+) → NITRITO (NO2-) → NITRATO (NO3-)
 *                   tóxico              muy tóxico         poco tóxico
 *                            ↑ bacterias nitrificantes ↑
 *
 * Leer un solo parámetro engaña; lo que informa es la combinación:
 *
 *   · Amoníaco total alto + nitrito bajo → piscina nueva, el filtro biológico aún no
 *     arranca. Las bacterias todavía no están.
 *   · Nitrito alto                → ciclo a medio hacer. Es el momento más
 *     peligroso: el nitrito impide a la sangre transportar oxígeno.
 *   · Solo nitrato alto           → ciclo sano pero agua vieja: toca recambio.
 *   · Amoníaco total y nitrito altos a la vez → el filtro no da abasto para la carga.
 *
 * RANGOS DE REFERENCIA (ajustables por el equipo de Acuicultura FIMCM):
 *
 *   pH
 *     VERDE     6.5 – 8.5
 *     AMARILLO  6.0 – 6.4  y  8.6 – 9.0
 *     ROJO      < 6.0  o  > 9.0
 *
 *   Nitrito (ppm)        Amoníaco total (ppm)       Nitrato (ppm)
 *     VERDE     < 0.5         VERDE     < 0.5          VERDE     < 50
 *     AMARILLO  0.5 – 1.0     AMARILLO  0.5 – 1.0      AMARILLO  50 – 100
 *     ROJO      > 1.0         ROJO      > 1.0          ROJO      > 100
 *
 * La estimación de NH3 conserva la regla provisional existente: usa pKa 9.25,
 * equivalente aproximadamente a 25 °C. Como v1.4 no mide temperatura, esta
 * salida debe entenderse como orientación y no como concentración confirmada.
 */
public final class EvaluadorSemaforo {

    // ---- Umbrales de pH ----
    public static final double PH_OPTIMO_MIN = 6.5;
    public static final double PH_OPTIMO_MAX = 8.5;
    public static final double PH_CRITICO_MIN = 6.0;
    public static final double PH_CRITICO_MAX = 9.0;

    // ---- Umbrales de nitrito (ppm) ----
    public static final double NITRITO_PRECAUCION = 0.5;
    public static final double NITRITO_CRITICO = 1.0;

    // ---- Umbrales de amoníaco total (ppm) ----
    public static final double AMONIACO_TOTAL_PRECAUCION = 0.5;
    public static final double AMONIACO_TOTAL_CRITICO = 1.0;

    // ---- Umbrales de nitrato (ppm) ----
    public static final double NITRATO_PRECAUCION = 50.0;
    public static final double NITRATO_CRITICO = 100.0;

    /**
     * Umbrales provisionales de amoníaco no ionizado NH3 estimado (ppm).
     * Por debajo de 0.02 no hay daño apreciable; por encima de 0.05 hay daño
     * agudo en branquias aunque el amoníaco total parezca aceptable.
     */
    public static final double NH3_PRECAUCION = 0.02;
    public static final double NH3_CRITICO = 0.05;

    private EvaluadorSemaforo() { }

    /** Punto de entrada principal: evalúa una medición completa de agua. */
    public static ResultadoSemaforo evaluar(String piscina, String fecha,
                                             double ph, double nitrato,
                                             double nitrito, double amoniacoTotal) {
        ResultadoSemaforo resultado = new ResultadoSemaforo(piscina, fecha);

        resultado.agregar(evaluarPh(ph));
        resultado.agregar(evaluarNitrito(nitrito));
        resultado.agregar(evaluarAmoniacoTotal(amoniacoTotal));
        resultado.agregar(evaluarNitrato(nitrato));
        resultado.agregar(evaluarAmoniacoNoIonizadoEstimado(ph, amoniacoTotal));

        LecturaEvaluada ciclo = evaluarCicloNitrogeno(amoniacoTotal, nitrito);
        if (ciclo != null) {
            resultado.agregar(ciclo);
        }

        return resultado;
    }

    // ======================= pH =======================

    public static LecturaEvaluada evaluarPh(double ph) {
        EstadoAlerta estado;
        String diagnostico;
        String recomendacion;

        if (ph < PH_CRITICO_MIN) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "Agua demasiado ácida. Daña las branquias y frena el crecimiento.";
            recomendacion = "Encalar la piscina y recambiar agua parcialmente. "
                    + "Avisar al técnico de la ESPOL.";
        } else if (ph > PH_CRITICO_MAX) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "Agua demasiado alcalina. Además aumenta la fracción tóxica del amoníaco total.";
            recomendacion = "Recambiar agua de inmediato y suspender la alimentación del día.";
        } else if (ph < PH_OPTIMO_MIN || ph > PH_OPTIMO_MAX) {
            estado = EstadoAlerta.AMARILLO;
            diagnostico = "pH ligeramente fuera del rango ideal.";
            recomendacion = "Revisar el encalado y volver a medir en 24 horas.";
        } else {
            estado = EstadoAlerta.VERDE;
            diagnostico = "pH dentro del rango ideal.";
            recomendacion = "Mantener el manejo actual.";
        }

        return new LecturaEvaluada("pH", ph, "", estado, diagnostico, recomendacion);
    }

    // ======================= NITRITO =======================

    public static LecturaEvaluada evaluarNitrito(double nitrito) {
        EstadoAlerta estado;
        String diagnostico;
        String recomendacion;

        if (nitrito > NITRITO_CRITICO) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "Nitrito crítico. La sangre del pez pierde la capacidad de "
                    + "llevar oxígeno: se asfixia aunque el agua tenga oxígeno de sobra.";
            recomendacion = "Recambiar la mitad del agua hoy mismo y no alimentar. "
                    + "Volver a medir mañana.";
        } else if (nitrito >= NITRITO_PRECAUCION) {
            estado = EstadoAlerta.AMARILLO;
            diagnostico = "Nitrito en aumento. El filtro biológico va con retraso.";
            recomendacion = "Reducir la ración a la mitad y recambiar un tercio del agua.";
        } else {
            estado = EstadoAlerta.VERDE;
            diagnostico = "Nitrito en nivel seguro.";
            recomendacion = "Mantener el manejo actual.";
        }

        return new LecturaEvaluada("Nitrito", nitrito, "ppm", estado, diagnostico, recomendacion);
    }

    // ======================= AMONÍACO TOTAL =======================

    public static LecturaEvaluada evaluarAmoniacoTotal(double amoniacoTotal) {
        EstadoAlerta estado;
        String diagnostico;
        String recomendacion;

        if (amoniacoTotal > AMONIACO_TOTAL_CRITICO) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "Amoníaco total crítico. Puede dañar las branquias y reducir el consumo.";
            recomendacion = "Recambiar la mitad del agua y suspender la alimentación "
                    + "hasta que baje. El alimento no consumido es la causa más común.";
        } else if (amoniacoTotal >= AMONIACO_TOTAL_PRECAUCION) {
            estado = EstadoAlerta.AMARILLO;
            diagnostico = "Amoníaco total por encima de lo deseable.";
            recomendacion = "Retirar el alimento no consumido del fondo y reducir la ración.";
        } else {
            estado = EstadoAlerta.VERDE;
            diagnostico = "Amoníaco total en nivel seguro.";
            recomendacion = "Mantener el manejo actual.";
        }

        return new LecturaEvaluada("Amoníaco total", amoniacoTotal, "ppm",
                estado, diagnostico, recomendacion);
    }

    // ======================= NITRATO =======================

    public static LecturaEvaluada evaluarNitrato(double nitrato) {
        EstadoAlerta estado;
        String diagnostico;
        String recomendacion;

        if (nitrato > NITRATO_CRITICO) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "Nitrato muy acumulado. El agua lleva demasiado tiempo sin renovarse.";
            recomendacion = "Programar un recambio grande de agua esta semana.";
        } else if (nitrato >= NITRATO_PRECAUCION) {
            estado = EstadoAlerta.AMARILLO;
            diagnostico = "Nitrato en aumento. Señal de que el agua se está envejeciendo.";
            recomendacion = "Planificar un recambio parcial en los próximos días.";
        } else {
            estado = EstadoAlerta.VERDE;
            diagnostico = "Nitrato en nivel seguro. Es la señal de un ciclo sano.";
            recomendacion = "Mantener el manejo actual.";
        }

        return new LecturaEvaluada("Nitrato", nitrato, "ppm", estado, diagnostico, recomendacion);
    }

    // ========== AMONÍACO NO IONIZADO ESTIMADO (pH × AMONÍACO TOTAL) ==========

    /**
     * Estima la fracción del amoníaco total que estaría como NH3 no ionizado.
     *
     * NH3 / (NH3 + NH4+) = 1 / (1 + 10^(pKa - pH)), con pKa ≈ 9.25 a 25 °C,
     * valor aproximado a 25 °C. La temperatura real no se registra en v1.4,
     * por lo que el resultado no es una medición confirmada de NH3:
     *
     *     pH 7 → 0.6 % del amoníaco total se estima como NH3
     *     pH 8 → 5.4 %
     *     pH 9 → 36 %
     *
     * Es decir, la MISMA lectura del kit es unas sesenta veces más peligrosa a
     * pH 9 que a pH 7. Por eso la lectura total nunca se juzga sola.
     */
    public static double amoniacoNoIonizadoEstimado(double ph, double amoniacoTotal) {
        final double pKa = 9.25;
        return amoniacoTotal / (1d + Math.pow(10d, pKa - ph));
    }

    public static LecturaEvaluada evaluarAmoniacoNoIonizadoEstimado(
            double ph, double amoniacoTotal) {
        double nh3 = amoniacoNoIonizadoEstimado(ph, amoniacoTotal);

        EstadoAlerta estado;
        String diagnostico;
        String recomendacion;

        if (nh3 > NH3_CRITICO) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "La estimación provisional de NH3 supera el nivel crítico.";
            recomendacion = "Recambiar agua de inmediato y no alimentar. "
                    + "Bajar el pH reduce la toxicidad al instante.";
        } else if (nh3 > NH3_PRECAUCION) {
            estado = EstadoAlerta.AMARILLO;
            diagnostico = "El pH eleva la fracción estimada de NH3 aunque el total parezca aceptable.";
            recomendacion = "No encalar por ahora: subir el pH empeoraría esto. "
                    + "Recambiar un tercio del agua.";
        } else {
            estado = EstadoAlerta.VERDE;
            diagnostico = "La estimación provisional de NH3 está por debajo de precaución.";
            recomendacion = "Mantener el manejo actual.";
        }

        return new LecturaEvaluada("Amoníaco no ionizado estimado", nh3, "ppm",
                estado, diagnostico, recomendacion);
    }

    // ======================= ESTADO DEL CICLO =======================

    /**
     * Interpreta amoníaco total y nitrito juntos para decir en qué punto está el ciclo.
     * Devuelve null cuando no hay nada que reportar más allá de los parámetros
     * sueltos.
     */
    public static LecturaEvaluada evaluarCicloNitrogeno(double amoniacoTotal, double nitrito) {
        boolean amoniacoTotalAlto = amoniacoTotal >= AMONIACO_TOTAL_PRECAUCION;
        boolean nitritoAlto = nitrito >= NITRITO_PRECAUCION;

        if (amoniacoTotalAlto && nitritoAlto) {
            return new LecturaEvaluada("Ciclo del nitrógeno", nitrito, "ppm",
                    EstadoAlerta.ROJO,
                    "Amoníaco total y nitrito altos a la vez: las bacterias no dan abasto con "
                            + "la carga de la piscina. Los dos venenos están presentes.",
                    "Suspender la alimentación, recambiar la mitad del agua y no sembrar "
                            + "más peces hasta que ambos bajen.");
        }

        if (amoniacoTotalAlto && nitrito < NITRITO_PRECAUCION) {
            return new LecturaEvaluada("Ciclo del nitrógeno", amoniacoTotal, "ppm",
                    EstadoAlerta.AMARILLO,
                    "Sube el amoníaco total pero el nitrito sigue bajo: el filtro biológico "
                            + "todavía no arranca. Es lo normal en una piscina recién llenada.",
                    "Alimentar poco durante una o dos semanas y medir cada dos días. "
                            + "Las bacterias se establecen solas.");
        }

        return null;
    }

}
