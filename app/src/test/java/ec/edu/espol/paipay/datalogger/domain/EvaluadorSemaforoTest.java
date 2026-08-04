package ec.edu.espol.paipay.datalogger.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;

/**
 * Pruebas del Semáforo de Alertas.
 *
 * Se ejecutan sin emulador (Run 'EvaluadorSemaforoTest' en Android Studio, o
 * ./gradlew test) porque la lógica es Java puro, sin dependencias de Android.
 * Verifican especialmente los BORDES de cada umbral, que es donde un semáforo
 * mal programado engaña al productor.
 */
public class EvaluadorSemaforoTest {

    // ------------------------- TEMPERATURA -------------------------

    @Test
    public void temperatura_enRangoOptimo_esVerde() {
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarTemperatura(27.0).estado);
    }

    @Test
    public void temperatura_enLosBordesDelRangoOptimo_siguenSiendoVerde() {
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarTemperatura(24.0).estado);
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarTemperatura(30.0).estado);
    }

    @Test
    public void temperatura_apenasFueraDelOptimo_esAmarilla() {
        assertEquals(EstadoAlerta.AMARILLO, EvaluadorSemaforo.evaluarTemperatura(23.9).estado);
        assertEquals(EstadoAlerta.AMARILLO, EvaluadorSemaforo.evaluarTemperatura(30.1).estado);
    }

    @Test
    public void temperatura_fueraDeLosLimitesCriticos_esRoja() {
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarTemperatura(19.9).estado);
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarTemperatura(32.1).estado);
    }

    // ------------------------- OXÍGENO -------------------------

    @Test
    public void oxigeno_igualAlUmbralOptimo_esVerde() {
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarOxigeno(5.0).estado);
    }

    @Test
    public void oxigeno_entreTresYCinco_esAmarillo() {
        assertEquals(EstadoAlerta.AMARILLO, EvaluadorSemaforo.evaluarOxigeno(3.0).estado);
        assertEquals(EstadoAlerta.AMARILLO, EvaluadorSemaforo.evaluarOxigeno(4.9).estado);
    }

    @Test
    public void oxigeno_bajoTres_esRojo() {
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarOxigeno(2.9).estado);
    }

    // ------------------------- RIESGO COMBINADO -------------------------

    @Test
    public void aguaCalienteConOxigenoBajo_disparaAlertaCombinadaRoja() {
        LecturaEvaluada combinada = EvaluadorSemaforo.evaluarRiesgoCombinado(31.0, 4.5);
        assertNotNull("31 °C con 4.5 mg/L debe generar alerta combinada", combinada);
        assertEquals(EstadoAlerta.ROJO, combinada.estado);
    }

    @Test
    public void aguaFrescaConBuenOxigeno_noGeneraAlertaCombinada() {
        assertNull(EvaluadorSemaforo.evaluarRiesgoCombinado(26.0, 7.0));
    }

    // ------------------------- ESTADO GLOBAL -------------------------

    @Test
    public void estadoGlobal_esElPeorDeLosParametros() {
        // Temperatura perfecta pero oxígeno crítico: la piscina está en rojo.
        RegistroAgua medicion = new RegistroAgua();
        medicion.piscina = "P-01";
        medicion.fechaMuestreo = "2026-08-04";
        medicion.temperaturaC = 27.0;
        medicion.oxigenoMgL = 2.0;

        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluar(medicion).getEstadoGlobal());
    }

    @Test
    public void condicionesIdeales_danVerde() {
        RegistroAgua medicion = new RegistroAgua();
        medicion.piscina = "P-02";
        medicion.fechaMuestreo = "2026-08-04";
        medicion.temperaturaC = 27.0;
        medicion.oxigenoMgL = 6.5;
        medicion.ph = 7.2;

        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluar(medicion).getEstadoGlobal());
    }

    @Test
    public void phFueraDeRango_noSeIgnora() {
        RegistroAgua medicion = new RegistroAgua();
        medicion.piscina = "P-03";
        medicion.fechaMuestreo = "2026-08-04";
        medicion.temperaturaC = 27.0;
        medicion.oxigenoMgL = 6.5;
        medicion.ph = 9.5;

        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluar(medicion).getEstadoGlobal());
    }

    // ------------------------- SEVERIDAD -------------------------

    @Test
    public void peor_devuelveSiempreElEstadoMasGrave() {
        assertEquals(EstadoAlerta.ROJO,
                EstadoAlerta.peor(EstadoAlerta.VERDE, EstadoAlerta.ROJO));
        assertEquals(EstadoAlerta.AMARILLO,
                EstadoAlerta.peor(EstadoAlerta.AMARILLO, EstadoAlerta.VERDE));
        assertEquals(EstadoAlerta.VERDE,
                EstadoAlerta.peor(EstadoAlerta.SIN_DATO, EstadoAlerta.VERDE));
    }
}
