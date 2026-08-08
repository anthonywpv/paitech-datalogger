package ec.edu.espol.paipay.datalogger.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class FechaUtilTest {
    @Test public void conservaElInstanteEntreGuayaquilYUtc() {
        long invalido = Long.MIN_VALUE;
        long instante = FechaUtil.millisDesdeIsoUtc(
                "2026-08-07T15:00:00-05:00", invalido);

        assertNotEquals(invalido, instante);
        assertEquals("2026-08-07T20:00:00Z", FechaUtil.isoUtc(instante));
        assertEquals("07/08/2026 15:00", FechaUtil.conHora(instante));
    }

    @Test public void aceptaFraccionesYZonaConDosPuntos() {
        long invalido = Long.MIN_VALUE;
        long instante = FechaUtil.millisDesdeIsoUtc(
                "2026-08-07T20:00:00.123456+00:00", invalido);

        assertNotEquals(invalido, instante);
        assertEquals("07/08/2026 15:00", FechaUtil.conHora(instante));
    }
}
