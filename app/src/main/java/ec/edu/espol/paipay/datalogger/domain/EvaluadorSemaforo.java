package ec.edu.espol.paipay.datalogger.domain;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;

/**
 * ===========================================================================
 *  SEMÁFORO DE ALERTAS — LÓGICA DE NEGOCIO (BACKEND EN JAVA)
 * ===========================================================================
 *
 * Evalúa temperatura y oxígeno disuelto de una piscina y devuelve VERDE,
 * AMARILLO o ROJO con un diagnóstico y una recomendación en lenguaje sencillo,
 * pensado para productores con baja alfabetización digital.
 *
 * Especie objetivo: Vieja Azul (Andinoacara rivulatus), cíclido nativo de la
 * cuenca del Guayas, tolerante pero sensible a hipoxia nocturna en piscinas
 * artesanales poco profundas.
 *
 * RANGOS DE REFERENCIA (ajustables por el equipo de Acuicultura FIMCM):
 *
 *   Temperatura (°C)
 *     VERDE     24.0 – 30.0   rango óptimo de crecimiento
 *     AMARILLO  20.0 – 23.9  y  30.1 – 32.0
 *     ROJO      < 20.0  o  > 32.0
 *
 *   Oxígeno disuelto (mg/L)
 *     VERDE     >= 5.0
 *     AMARILLO  3.0 – 4.9
 *     ROJO      < 3.0
 *
 *   pH (opcional, no altera el estado global)
 *     VERDE     6.5 – 8.5
 *     AMARILLO  6.0 – 6.4  y  8.6 – 9.0
 *     ROJO      < 6.0  o  > 9.0
 *
 * IMPORTANTE: la temperatura y el oxígeno están acoplados. El agua caliente
 * retiene menos oxígeno, por eso una piscina a 31 °C con 4.5 mg/L es más
 * riesgosa de lo que sugiere cada parámetro por separado; ese acoplamiento se
 * refleja en el método evaluarRiesgoCombinado().
 */
public final class EvaluadorSemaforo {

    // ---- Umbrales de temperatura ----
    public static final double TEMP_OPTIMA_MIN = 24.0;
    public static final double TEMP_OPTIMA_MAX = 30.0;
    public static final double TEMP_CRITICA_MIN = 20.0;
    public static final double TEMP_CRITICA_MAX = 32.0;

    // ---- Umbrales de oxígeno disuelto ----
    public static final double OD_OPTIMO_MIN = 5.0;
    public static final double OD_CRITICO_MIN = 3.0;

    // ---- Umbrales de pH ----
    public static final double PH_OPTIMO_MIN = 6.5;
    public static final double PH_OPTIMO_MAX = 8.5;
    public static final double PH_CRITICO_MIN = 6.0;
    public static final double PH_CRITICO_MAX = 9.0;

    private EvaluadorSemaforo() { }

    /** Punto de entrada principal: evalúa una medición completa de agua. */
    public static ResultadoSemaforo evaluar(RegistroAgua registro) {
        ResultadoSemaforo resultado =
                new ResultadoSemaforo(registro.piscina, registro.fechaMuestreo);

        resultado.agregar(evaluarTemperatura(registro.temperaturaC));
        resultado.agregar(evaluarOxigeno(registro.oxigenoMgL));

        if (registro.ph != null) {
            resultado.agregar(evaluarPh(registro.ph));
        }

        LecturaEvaluada combinada =
                evaluarRiesgoCombinado(registro.temperaturaC, registro.oxigenoMgL);
        if (combinada != null) {
            resultado.agregar(combinada);
        }

        return resultado;
    }

    // ======================= TEMPERATURA =======================

    public static LecturaEvaluada evaluarTemperatura(double t) {
        EstadoAlerta estado;
        String diagnostico;
        String recomendacion;

        if (t < TEMP_CRITICA_MIN) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "Agua demasiado fría. El pez deja de comer y crece muy lento.";
            recomendacion = "Suspender o reducir la alimentación y revisar el nivel del agua. "
                    + "Piscinas más profundas pierden menos calor en la noche.";
        } else if (t > TEMP_CRITICA_MAX) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "Agua demasiado caliente. Riesgo de estrés térmico y mortalidad.";
            recomendacion = "Aumentar el nivel del agua, dar sombra a la piscina y alimentar "
                    + "solo temprano en la mañana.";
        } else if (t < TEMP_OPTIMA_MIN) {
            estado = EstadoAlerta.AMARILLO;
            diagnostico = "Temperatura por debajo del rango ideal.";
            recomendacion = "Reducir la ración diaria y volver a medir en la tarde.";
        } else if (t > TEMP_OPTIMA_MAX) {
            estado = EstadoAlerta.AMARILLO;
            diagnostico = "Temperatura por encima del rango ideal.";
            recomendacion = "Vigilar el oxígeno: el agua caliente retiene menos oxígeno.";
        } else {
            estado = EstadoAlerta.VERDE;
            diagnostico = "Temperatura dentro del rango óptimo de crecimiento.";
            recomendacion = "Mantener el manejo actual.";
        }

        return new LecturaEvaluada("Temperatura", t, "°C", estado, diagnostico, recomendacion);
    }

    // ======================= OXÍGENO DISUELTO =======================

    public static LecturaEvaluada evaluarOxigeno(double od) {
        EstadoAlerta estado;
        String diagnostico;
        String recomendacion;

        if (od < OD_CRITICO_MIN) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "Oxígeno crítico. Los peces pueden estar boqueando en la superficie.";
            recomendacion = "Acción inmediata: recambiar agua o encender la bomba para airear. "
                    + "No alimentar hasta que el oxígeno suba.";
        } else if (od < OD_OPTIMO_MIN) {
            estado = EstadoAlerta.AMARILLO;
            diagnostico = "Oxígeno bajo. El crecimiento se frena aunque el pez no muera.";
            recomendacion = "Airear en las primeras horas de la mañana y retirar el alimento "
                    + "no consumido del fondo.";
        } else {
            estado = EstadoAlerta.VERDE;
            diagnostico = "Oxígeno disuelto adecuado.";
            recomendacion = "Mantener el manejo actual.";
        }

        return new LecturaEvaluada("Oxígeno disuelto", od, "mg/L", estado, diagnostico, recomendacion);
    }

    // ======================= pH =======================

    public static LecturaEvaluada evaluarPh(double ph) {
        EstadoAlerta estado;
        String diagnostico;
        String recomendacion;

        if (ph < PH_CRITICO_MIN || ph > PH_CRITICO_MAX) {
            estado = EstadoAlerta.ROJO;
            diagnostico = "pH fuera del rango tolerable para la especie.";
            recomendacion = "Recambiar agua parcialmente y avisar al técnico de la ESPOL.";
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

    // ======================= RIESGO COMBINADO =======================

    /**
     * Regla acoplada temperatura + oxígeno.
     *
     * A mayor temperatura, menor solubilidad del oxígeno y mayor tasa metabólica
     * del pez: la demanda sube justo cuando la oferta baja. Por eso un oxígeno
     * "aceptable" deja de serlo cuando el agua pasa de 30 °C.
     *
     * Devuelve null cuando no hay una interacción que reportar.
     */
    public static LecturaEvaluada evaluarRiesgoCombinado(double temperatura, double oxigeno) {
        boolean aguaCaliente = temperatura > TEMP_OPTIMA_MAX;
        boolean oxigenoJusto = oxigeno < 6.0;

        if (aguaCaliente && oxigeno < OD_OPTIMO_MIN) {
            return new LecturaEvaluada(
                    "Riesgo combinado", oxigeno, "mg/L",
                    EstadoAlerta.ROJO,
                    "Agua caliente con oxígeno bajo: la peor combinación posible. "
                            + "El pez necesita más oxígeno del que el agua puede darle.",
                    "Airear de inmediato y suspender la alimentación del día. "
                            + "Medir de nuevo al amanecer, que es la hora de menor oxígeno.");
        }

        if (aguaCaliente && oxigenoJusto) {
            return new LecturaEvaluada(
                    "Riesgo combinado", oxigeno, "mg/L",
                    EstadoAlerta.AMARILLO,
                    "El agua está caliente y el oxígeno apenas alcanza. "
                            + "De madrugada podría bajar a nivel crítico.",
                    "Programar una medición al amanecer y tener lista la bomba de aireación.");
        }

        return null;
    }
}
