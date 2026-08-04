package ec.edu.espol.paipay.datalogger.domain;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;

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
 *     peces  →  AMONIO (NH4+/NH3)  →  NITRITO (NO2-)  →  NITRATO (NO3-)
 *                   tóxico              muy tóxico         poco tóxico
 *                            ↑ bacterias nitrificantes ↑
 *
 * Leer un solo parámetro engaña; lo que informa es la combinación:
 *
 *   · Amonio alto + nitrito bajo  → piscina nueva, el filtro biológico aún no
 *     arranca. Las bacterias todavía no están.
 *   · Nitrito alto                → ciclo a medio hacer. Es el momento más
 *     peligroso: el nitrito impide a la sangre transportar oxígeno.
 *   · Solo nitrato alto           → ciclo sano pero agua vieja: toca recambio.
 *   · Amonio y nitrito altos a la vez → el filtro no da abasto para la carga.
 *
 * RANGOS DE REFERENCIA (ajustables por el equipo de Acuicultura FIMCM):
 *
 *   pH
 *     VERDE     6.5 – 8.5
 *     AMARILLO  6.0 – 6.4  y  8.6 – 9.0
 *     ROJO      < 6.0  o  > 9.0
 *
 *   Nitrito (mg/L)          Amonio total (mg/L)      Nitrato (mg/L)
 *     VERDE     < 0.5         VERDE     < 0.5          VERDE     < 50
 *     AMARILLO  0.5 – 1.0     AMARILLO  0.5 – 1.0      AMARILLO  50 – 100
 *     ROJO      > 1.0         ROJO      > 1.0          ROJO      > 100
 *
 * IMPORTANTE: el amonio y el pH están acoplados. Lo que mata no es el amonio
 * total que marca el kit, sino la fracción en forma de amoníaco libre (NH3),
 * que crece de golpe con el pH. Ese acoplamiento vive en amoniacoLibre() y en
 * evaluarAmoniacoLibre(), y es el equivalente a la vieja regla de temperatura
 * y oxígeno.
 */
public final class EvaluadorSemaforo {

    // ---- Umbrales de pH ----
    public static final double PH_OPTIMO_MIN = 6.5;
    public static final double PH_OPTIMO_MAX = 8.5;
    public static final double PH_CRITICO_MIN = 6.0;
    public static final double PH_CRITICO_MAX = 9.0;

    // ---- Umbrales de nitrito (mg/L) ----
    public static final double NITRITO_PRECAUCION = 0.5;
    public static final double NITRITO_CRITICO = 1.0;

    // ---- Umbrales de amonio total (mg/L) ----
    public static final double AMONIO_PRECAUCION = 0.5;
    public static final double AMONIO_CRITICO = 1.0;

    // ---- Umbrales de nitrato (mg/L) ----
    public static final double NITRATO_PRECAUCION = 50.0;
    public static final double NITRATO_CRITICO = 100.0;

    /**
     * Umbrales de amoníaco libre NH3 (mg/L), el tóxico de verdad.
     * Por debajo de 0.02 no hay daño apreciable; por encima de 0.05 hay daño
     * agudo en branquias aunque el amonio total parezca aceptable.
     */
    public static final double NH3_PRECAUCION = 0.02;
    public static final double NH3_CRITICO = 0.05;

    /** Caída de población entre muestreos que se considera mortalidad. */
    public static final double MORTALIDAD_PRECAUCION = 0.10;   // 10 %
    public static final double MORTALIDAD_CRITICA = 0.25;      // 25 %

    private EvaluadorSemaforo() { }

    /** Punto de entrada principal: evalúa una medición completa de agua. */
    public static ResultadoSemaforo evaluar(RegistroAgua registro) {
        ResultadoSemaforo resultado =
                new ResultadoSemaforo(registro.piscina, registro.fechaMuestreo);

        resultado.agregar(evaluarPh(registro.ph));
        resultado.agregar(evaluarNitrito(registro.nitritoMgL));
        resultado.agregar(evaluarAmonio(registro.amonioMgL));
        resultado.agregar(evaluarNitrato(registro.nitratoMgL));

        resultado.agregar(evaluarAmoniacoLibre(registro.ph, registro.amonioMgL));

        LecturaEvaluada ciclo = evaluarCicloNitrogeno(registro.amonioMgL, registro.nitritoMgL);
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
            diagnostico = "Agua demasiado alcalina. Además vuelve mucho más tóxico el amonio.";
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

        return new LecturaEvaluada("Nitrito", nitrito, "mg/L", estado, diagnostico, recomendacion);
    }

    // ======================= AMONIO TOTAL =======================

    public static LecturaEvaluada evaluarAmonio(double amonio) {
        EstadoAlerta estado;
        String diagnostico;
        String recomendacion;

        if (amonio > AMONIO_CRITICO) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "Amonio crítico. Quema las branquias y el pez deja de comer.";
            recomendacion = "Recambiar la mitad del agua y suspender la alimentación "
                    + "hasta que baje. El alimento no consumido es la causa más común.";
        } else if (amonio >= AMONIO_PRECAUCION) {
            estado = EstadoAlerta.AMARILLO;
            diagnostico = "Amonio por encima de lo deseable.";
            recomendacion = "Retirar el alimento no consumido del fondo y reducir la ración.";
        } else {
            estado = EstadoAlerta.VERDE;
            diagnostico = "Amonio en nivel seguro.";
            recomendacion = "Mantener el manejo actual.";
        }

        return new LecturaEvaluada("Amonio", amonio, "mg/L", estado, diagnostico, recomendacion);
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

        return new LecturaEvaluada("Nitrato", nitrato, "mg/L", estado, diagnostico, recomendacion);
    }

    // ======================= AMONÍACO LIBRE (pH × AMONIO) =======================

    /**
     * Fracción del amonio total que está como amoníaco libre NH3, que es la
     * forma que atraviesa la branquia y envenena al pez.
     *
     * NH3 / (NH3 + NH4+) = 1 / (1 + 10^(pKa - pH)), con pKa ≈ 9.25 a 25 °C,
     * temperatura representativa de las piscinas del recinto. Se fija la
     * temperatura porque ya no se mide; el error que introduce es pequeño al
     * lado del efecto del pH, que es el que domina:
     *
     *     pH 7 → 0.6 % del amonio es tóxico
     *     pH 8 → 5.4 %
     *     pH 9 → 36 %
     *
     * Es decir, la MISMA lectura del kit es unas sesenta veces más peligrosa a
     * pH 9 que a pH 7. Por eso el amonio nunca se juzga solo.
     */
    public static double amoniacoLibre(double ph, double amonioTotal) {
        final double pKa = 9.25;
        return amonioTotal / (1d + Math.pow(10d, pKa - ph));
    }

    public static LecturaEvaluada evaluarAmoniacoLibre(double ph, double amonioTotal) {
        double nh3 = amoniacoLibre(ph, amonioTotal);

        EstadoAlerta estado;
        String diagnostico;
        String recomendacion;

        if (nh3 > NH3_CRITICO) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "Amoníaco libre en nivel tóxico. Con este pH, el amonio medido "
                    + "está en su forma que envenena.";
            recomendacion = "Recambiar agua de inmediato y no alimentar. "
                    + "Bajar el pH reduce la toxicidad al instante.";
        } else if (nh3 > NH3_PRECAUCION) {
            estado = EstadoAlerta.AMARILLO;
            diagnostico = "El pH está volviendo tóxico el amonio presente, aunque el "
                    + "total parezca aceptable.";
            recomendacion = "No encalar por ahora: subir el pH empeoraría esto. "
                    + "Recambiar un tercio del agua.";
        } else {
            estado = EstadoAlerta.VERDE;
            diagnostico = "El amonio presente está en su forma inofensiva.";
            recomendacion = "Mantener el manejo actual.";
        }

        return new LecturaEvaluada("Amoníaco libre", nh3, "mg/L",
                estado, diagnostico, recomendacion);
    }

    // ======================= ESTADO DEL CICLO =======================

    /**
     * Interpreta amonio y nitrito juntos para decir en qué punto está el ciclo.
     * Devuelve null cuando no hay nada que reportar más allá de los parámetros
     * sueltos.
     */
    public static LecturaEvaluada evaluarCicloNitrogeno(double amonio, double nitrito) {
        boolean amonioAlto = amonio >= AMONIO_PRECAUCION;
        boolean nitritoAlto = nitrito >= NITRITO_PRECAUCION;

        if (amonioAlto && nitritoAlto) {
            return new LecturaEvaluada("Ciclo del nitrógeno", nitrito, "mg/L",
                    EstadoAlerta.ROJO,
                    "Amonio y nitrito altos a la vez: las bacterias no dan abasto con "
                            + "la carga de la piscina. Los dos venenos están presentes.",
                    "Suspender la alimentación, recambiar la mitad del agua y no sembrar "
                            + "más peces hasta que ambos bajen.");
        }

        if (amonioAlto && nitrito < NITRITO_PRECAUCION) {
            return new LecturaEvaluada("Ciclo del nitrógeno", amonio, "mg/L",
                    EstadoAlerta.AMARILLO,
                    "Sube el amonio pero el nitrito sigue bajo: el filtro biológico "
                            + "todavía no arranca. Es lo normal en una piscina recién llenada.",
                    "Alimentar poco durante una o dos semanas y medir cada dos días. "
                            + "Las bacterias se establecen solas.");
        }

        return null;
    }

    // ======================= MORTALIDAD ENTRE MUESTREOS =======================

    /**
     * Compara la población estimada con la del muestreo anterior de la misma
     * piscina. Una caída fuerte es la señal más clara de que algo va mal,
     * aunque todos los parámetros del agua salgan en verde el día de la visita:
     * el productor pudo llegar después del episodio.
     *
     * Devuelve null si falta alguno de los dos conteos o si no hubo caída.
     */
    public static LecturaEvaluada evaluarMortalidad(Integer poblacionAnterior,
                                                    Integer poblacionActual) {
        if (poblacionAnterior == null || poblacionActual == null) return null;
        if (poblacionAnterior <= 0 || poblacionActual >= poblacionAnterior) return null;

        int perdidos = poblacionAnterior - poblacionActual;
        double caida = (double) perdidos / poblacionAnterior;

        if (caida >= MORTALIDAD_CRITICA) {
            return new LecturaEvaluada("Mortalidad", perdidos, "peces",
                    EstadoAlerta.ROJO,
                    "Se perdió más de la cuarta parte de la población desde el muestreo "
                            + "anterior (" + poblacionAnterior + " → " + poblacionActual + ").",
                    "Revisar la piscina hoy: buscar peces muertos, medir el agua otra vez "
                            + "y avisar al técnico de la ESPOL.");
        }

        if (caida >= MORTALIDAD_PRECAUCION) {
            return new LecturaEvaluada("Mortalidad", perdidos, "peces",
                    EstadoAlerta.AMARILLO,
                    "Bajó la población desde el muestreo anterior ("
                            + poblacionAnterior + " → " + poblacionActual + ").",
                    "Vigilar de cerca y medir el agua con más frecuencia esta semana.");
        }

        return null;
    }
}
