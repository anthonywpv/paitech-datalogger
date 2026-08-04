package ec.edu.espol.paipay.datalogger.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;

/**
 * Pruebas del Semáforo de Alertas.
 *
 * Se ejecutan sin emulador porque la lógica es Java puro. Verifican sobre todo
 * los BORDES de cada umbral, que es donde un semáforo mal programado engaña al
 * productor, y el acoplamiento pH–amonio, que es lo que un parámetro leído por
 * separado no puede detectar.
 */
public class EvaluadorSemaforoTest {

    private static RegistroAgua medicion(double ph, double nitrato,
                                         double nitrito, double amonio) {
        RegistroAgua r = new RegistroAgua();
        r.piscina = "P-01";
        r.fechaMuestreo = "2026-08-04";
        r.ph = ph;
        r.nitratoMgL = nitrato;
        r.nitritoMgL = nitrito;
        r.amonioMgL = amonio;
        return r;
    }

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

    // ------------------------- AMONIO -------------------------

    @Test
    public void amonio_bajoElUmbral_esVerde() {
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarAmonio(0.49).estado);
    }

    @Test
    public void amonio_sobreElCritico_esRojo() {
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarAmonio(1.01).estado);
    }

    // ------------------------- NITRATO -------------------------

    @Test
    public void nitrato_toleraValoresMuchoMasAltosQueElNitrito() {
        // Es el producto final del ciclo y el menos tóxico: 40 mg/L está bien,
        // mientras que 40 mg/L de nitrito sería letal.
        assertEquals(EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarNitrato(40).estado);
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarNitrito(40).estado);
    }

    @Test
    public void nitrato_muyAcumulado_esRojo() {
        assertEquals(EstadoAlerta.ROJO, EvaluadorSemaforo.evaluarNitrato(101).estado);
    }

    // ------------------------- AMONÍACO LIBRE (pH × AMONIO) -------------------------

    @Test
    public void amoniacoLibre_creceMuchoConElPh() {
        // Mismo amonio total, distinto pH: la fracción tóxica se dispara.
        double aPh7 = EvaluadorSemaforo.amoniacoLibre(7.0, 1.0);
        double aPh9 = EvaluadorSemaforo.amoniacoLibre(9.0, 1.0);
        assertTrue("a pH 9 debe haber mucho más NH3 que a pH 7", aPh9 > aPh7 * 10);
    }

    @Test
    public void amoniacoLibre_coincideConLasTablasPublicadas() {
        // A 25 °C: ~0.6 % a pH 7, ~5 % a pH 8, ~36 % a pH 9.
        assertEquals(0.006, EvaluadorSemaforo.amoniacoLibre(7.0, 1.0), 0.002);
        assertEquals(0.053, EvaluadorSemaforo.amoniacoLibre(8.0, 1.0), 0.01);
        assertEquals(0.360, EvaluadorSemaforo.amoniacoLibre(9.0, 1.0), 0.03);
    }

    /**
     * El caso que justifica toda la regla: un amonio que leído solo pasaría por
     * aceptable se vuelve peligroso cuando el pH está alto.
     */
    @Test
    public void amonioAceptableConPhAlto_disparaAlertaDeAmoniacoLibre() {
        assertEquals("0.4 mg/L de amonio total es VERDE por sí solo",
                EstadoAlerta.VERDE, EvaluadorSemaforo.evaluarAmonio(0.4).estado);

        LecturaEvaluada libre = EvaluadorSemaforo.evaluarAmoniacoLibre(9.0, 0.4);
        assertEquals("pero a pH 9 esa misma lectura es crítica",
                EstadoAlerta.ROJO, libre.estado);
    }

    @Test
    public void mismoAmonioConPhNeutro_noAlarma() {
        assertEquals(EstadoAlerta.VERDE,
                EvaluadorSemaforo.evaluarAmoniacoLibre(7.0, 0.4).estado);
    }

    // ------------------------- ESTADO DEL CICLO -------------------------

    @Test
    public void amonioAltoConNitritoBajo_indicaFiltroSinArrancar() {
        LecturaEvaluada ciclo = EvaluadorSemaforo.evaluarCicloNitrogeno(0.8, 0.1);
        assertNotNull(ciclo);
        assertEquals(EstadoAlerta.AMARILLO, ciclo.estado);
    }

    @Test
    public void amonioYNitritoAltosALaVez_esRojo() {
        LecturaEvaluada ciclo = EvaluadorSemaforo.evaluarCicloNitrogeno(0.8, 0.8);
        assertNotNull(ciclo);
        assertEquals(EstadoAlerta.ROJO, ciclo.estado);
    }

    @Test
    public void cicloEquilibrado_noGeneraAlerta() {
        assertNull(EvaluadorSemaforo.evaluarCicloNitrogeno(0.1, 0.1));
    }

    // ------------------------- MORTALIDAD -------------------------

    @Test
    public void caidaFuerteDePoblacion_esRoja() {
        LecturaEvaluada m = EvaluadorSemaforo.evaluarMortalidad(1000, 700);
        assertNotNull(m);
        assertEquals(EstadoAlerta.ROJO, m.estado);
    }

    @Test
    public void caidaLeveDePoblacion_esAmarilla() {
        LecturaEvaluada m = EvaluadorSemaforo.evaluarMortalidad(1000, 880);
        assertNotNull(m);
        assertEquals(EstadoAlerta.AMARILLO, m.estado);
    }

    @Test
    public void poblacionQueSubeOSeMantiene_noEsAlerta() {
        assertNull(EvaluadorSemaforo.evaluarMortalidad(1000, 1000));
        assertNull(EvaluadorSemaforo.evaluarMortalidad(1000, 1200));
    }

    @Test
    public void sinConteoAnterior_noSeInventaMortalidad() {
        assertNull(EvaluadorSemaforo.evaluarMortalidad(null, 500));
        assertNull(EvaluadorSemaforo.evaluarMortalidad(500, null));
    }

    // ------------------------- ESTADO GLOBAL -------------------------

    @Test
    public void estadoGlobal_esElPeorDeLosParametros() {
        // Todo perfecto salvo el nitrito: la piscina está en rojo.
        assertEquals(EstadoAlerta.ROJO,
                EvaluadorSemaforo.evaluar(medicion(7.2, 20, 2.0, 0.1)).getEstadoGlobal());
    }

    @Test
    public void condicionesIdeales_danVerde() {
        assertEquals(EstadoAlerta.VERDE,
                EvaluadorSemaforo.evaluar(medicion(7.2, 20, 0.1, 0.1)).getEstadoGlobal());
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
