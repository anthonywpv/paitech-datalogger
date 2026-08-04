package ec.edu.espol.paipay.datalogger.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas de las fechas: el código de muestreo y el parseo de lo que devuelve
 * PostgREST. Ambas son Java puro, así que corren sin emulador.
 */
public class FechaUtilTest {

    // ------------------------- CÓDIGO DE MUESTREO -------------------------

    @Test
    public void codigoMuestreo_usaFormatoDdMmAaaa() {
        assertEquals("M-04082026", FechaUtil.codigoMuestreo("2026-08-04"));
    }

    /**
     * El caso que rompería el agrupado: si el formateador no rellenara con cero,
     * el 4 de agosto daría "M-482026" y chocaría con otras fechas.
     */
    @Test
    public void codigoMuestreo_rellenaConCeroDiasYMesesDeUnDigito() {
        assertEquals("M-01012026", FechaUtil.codigoMuestreo("2026-01-01"));
        assertEquals("M-09092026", FechaUtil.codigoMuestreo("2026-09-09"));
        assertEquals(10, FechaUtil.codigoMuestreo("2026-01-01").length());
    }

    @Test
    public void codigoMuestreo_dosFechasDistintasDanCodigosDistintos() {
        assertTrue(!FechaUtil.codigoMuestreo("2026-08-04")
                .equals(FechaUtil.codigoMuestreo("2026-08-05")));
    }

    @Test
    public void codigoMuestreo_conFechaIlegibleNoRevienta() {
        assertEquals("M-no-es-fecha", FechaUtil.codigoMuestreo("no-es-fecha"));
    }

    // ------------------------- FECHAS DE POSTGREST -------------------------

    @Test
    public void millisDesdeIsoUtc_deshaceLoQueEscribeIsoUtc() {
        // Se compara al segundo porque isoUtc no serializa milisegundos.
        long original = 1785000000000L / 1000L * 1000L;
        String texto = FechaUtil.isoUtc(original);
        assertEquals(original, FechaUtil.millisDesdeIsoUtc(texto, -1L));
    }

    @Test
    public void millisDesdeIsoUtc_aceptaLosTresFormatosQueDevuelvePostgres() {
        long conZ = FechaUtil.millisDesdeIsoUtc("2026-08-04T17:30:00Z", -1L);
        long conOffset = FechaUtil.millisDesdeIsoUtc("2026-08-04T17:30:00+00:00", -1L);
        long conFraccion = FechaUtil.millisDesdeIsoUtc("2026-08-04T17:30:00.123456+00:00", -1L);

        assertTrue("ninguno debe caer al respaldo", conZ > 0);
        assertEquals(conZ, conOffset);
        assertEquals(conZ, conFraccion);
    }

    @Test
    public void millisDesdeIsoUtc_interpretaElDesfaseHorario() {
        long utc = FechaUtil.millisDesdeIsoUtc("2026-08-04T17:30:00Z", -1L);
        // Ecuador es UTC-5: las 12:30 locales son las 17:30 UTC.
        long guayaquil = FechaUtil.millisDesdeIsoUtc("2026-08-04T12:30:00-05:00", -1L);
        assertEquals(utc, guayaquil);
    }

    @Test
    public void millisDesdeIsoUtc_devuelveElRespaldoSiNoEntiendeElTexto() {
        assertEquals(99L, FechaUtil.millisDesdeIsoUtc("cualquier cosa", 99L));
        assertEquals(99L, FechaUtil.millisDesdeIsoUtc(null, 99L));
        assertEquals(99L, FechaUtil.millisDesdeIsoUtc("", 99L));
    }
}
