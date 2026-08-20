package ec.edu.espol.paipay.datalogger.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas del Semáforo de Alertas.
 *
 * Se ejecutan sin emulador porque la lógica es Java puro. Verifican sobre todo
 * los BORDES de cada umbral, que es donde un semáforo mal programado engaña al
 * productor, y la estimación provisional pH–amoníaco total.
 */
public class EvaluadorSemaforoTest {

    // ------------------------- pH -------------------------

    @Test
    public void ph_enRangoOptimo_esVerde() {
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarPh(7.2).estado);
    }

    @Test
    public void ph_enLosBordesDelRangoOptimo_sigueSiendoVerde() {
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarPh(6.5).estado);
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarPh(8.5).estado);
    }

    @Test
    public void ph_apenasFueraDelOptimo_esAmarillo() {
        assertEquals(EstadoAlerta.AMARILLO, EvaluadorSemaforo.evaluarPh(6.4).estado);
        assertEquals(EstadoAlerta.AMARILLO, EvaluadorSemaforo.evaluarPh(8.6).estado);
    }

    @Test
    public void ph_fueraDeLosLimitesCriticos_esRojo() {
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarPh(5.9).estado);
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarPh(9.1).estado);
    }

    // ------------------------- NITRITO -------------------------

    @Test
    public void nitrito_bajoElUmbral_esVerde() {
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarNitrito(0.49).estado);
    }

    @Test
    public void nitrito_enElUmbralDePrecaucion_esAmarillo() {
        assertEquals(EstadoAlerta.AMARILLO, EvaluadorSemaforo.evaluarNitrito(0.5).estado);
        assertEquals(EstadoAlerta.AMARILLO, EvaluadorSemaforo.evaluarNitrito(1.0).estado);
    }

    @Test
    public void nitrito_sobreElCritico_esRojo() {
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarNitrito(1.01).estado);
    }

    // ------------------------- AMONÍACO TOTAL -------------------------

    @Test
    public void amoniacoTotal_bajoElUmbral_esVerde() {
        assertEquals(EstadoAlerta.VERDE,
                EvaluadorSemaforo.evaluarAmoniacoTotal(0.49).estado);
    }

    @Test
    public void amoniacoTotal_sobreElCritico_esRojo() {
        assertEquals(EstadoAlerta.ROJO,
                EvaluadorSemaforo.evaluarAmoniacoTotal(1.01).estado);
    }

    // ------------------------- NITRATO -------------------------

    @Test
    public void nitrato_toleraValoresMuchoMasAltosQueElNitrito() {
        // Es el producto final del ciclo y el menos tóxico: 40 ppm está bien,
        // mientras que 40 ppm de nitrito activa rojo.
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarNitrato(40).estado);
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarNitrito(40).estado);
    }

    @Test
    public void nitrato_muyAcumulado_esRojo() {
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarNitrato(101).estado);
    }

    // -------- AMONÍACO NO IONIZADO ESTIMADO (pH × AMONÍACO TOTAL) --------

    @Test
    public void amoniacoNoIonizadoEstimado_creceMuchoConElPh() {
        double aPh7 = EvaluadorSemaforo.amoniacoNoIonizadoEstimado(7.0, 1.0);
        double aPh9 = EvaluadorSemaforo.amoniacoNoIonizadoEstimado(9.0, 1.0);
        assertTrue("a pH 9 debe haber mucho más NH3 que a pH 7", aPh9 > aPh7 * 10);
    }

    @Test
    public void amoniacoNoIonizadoEstimado_conservaLaFormulaProvisional() {
        // A 25 °C: ~0.6 % a pH 7, ~5 % a pH 8, ~36 % a pH 9.
        assertEquals(0.006, EvaluadorSemaforo.amoniacoNoIonizadoEstimado(7.0, 1.0), 0.002);
        assertEquals(0.053, EvaluadorSemaforo.amoniacoNoIonizadoEstimado(8.0, 1.0), 0.01);
        assertEquals(0.360, EvaluadorSemaforo.amoniacoNoIonizadoEstimado(9.0, 1.0), 0.03);
    }

    /**
     * El caso que justifica toda la regla: un amoníaco total que leído solo pasaría por
     * aceptable se vuelve peligroso cuando el pH está alto.
     */
    @Test
    public void amoniacoTotalAceptableConPhAlto_disparaEstimacionCritica() {
        assertEquals("0.4 ppm de amoníaco total es VERDE por sí solo",
                EstadoAlerta.VERDE,
                EvaluadorSemaforo.evaluarAmoniacoTotal(0.4).estado);

        LecturaEvaluada libre = EvaluadorSemaforo
                .evaluarAmoniacoNoIonizadoEstimado(9.0, 0.4);
        assertEquals("pero a pH 9 esa misma lectura es crítica",
                EstadoAlerta.ROJO, libre.estado);
    }

    @Test
    public void mismoAmoniacoTotalConPhNeutro_noAlarma() {
        assertEquals(EstadoAlerta.VERDE,
                EvaluadorSemaforo.evaluarAmoniacoNoIonizadoEstimado(7.0, 0.4).estado);
    }

    // ------------------------- ESTADO DEL CICLO -------------------------

    @Test
    public void amoniacoTotalAltoConNitritoBajo_indicaFiltroSinArrancar() {
        LecturaEvaluada ciclo = EvaluadorSemaforo.evaluarCicloNitrogeno(0.8, 0.1);
        assertNotNull(ciclo);
        assertEquals(EstadoAlerta.AMARILLO, ciclo.estado);
    }

    @Test
    public void amoniacoTotalYNitritoAltosALaVez_esRojo() {
        LecturaEvaluada ciclo = EvaluadorSemaforo.evaluarCicloNitrogeno(0.8, 0.8);
        assertNotNull(ciclo);
        assertEquals(EstadoAlerta.ROJO, ciclo.estado);
    }

    @Test
    public void cicloEquilibrado_noGeneraAlerta() {
        assertNull(EvaluadorSemaforo.evaluarCicloNitrogeno(0.1, 0.1));
    }

    // ------------------------- ESTADO GLOBAL -------------------------

    @Test
    public void estadoGlobal_esElPeorDeLosParametros() {
        // Todo perfecto salvo el nitrito: la piscina está en rojo.
        assertEquals(EstadoAlerta.ROJO,
                EvaluadorSemaforo.evaluar("P-01", "2026-08-04", 7.2, 20, 2.0, 0.1).getEstadoGlobal());
    }

    @Test
    public void condicionesIdeales_danVerde() {
        assertEquals(EstadoAlerta.VERDE,
                EvaluadorSemaforo.evaluar("P-01", "2026-08-04", 7.2, 20, 0.1, 0.1).getEstadoGlobal());
    }

    @Test
    public void capturaAguaAceptaDecimalesManualesYConservaLimites() {
        assertTrue(ValidadorAgua.esDecimalValido("7.31", 2, 14));
        assertTrue(ValidadorAgua.esDecimalValido("0,375", 3, 9_999_999.999));
        assertTrue(ValidadorAgua.esDecimalValido("12.375", 3, 9_999_999.999));
        assertTrue(ValidadorAgua.esDecimalValido("", 3, 9_999_999.999));
        assertFalse(ValidadorAgua.esDecimalValido("14.01", 2, 14));
        assertFalse(ValidadorAgua.esDecimalValido("-0.1", 3, 9_999_999.999));
        assertFalse(ValidadorAgua.esDecimalValido("0.1234", 3, 9_999_999.999));
    }

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
