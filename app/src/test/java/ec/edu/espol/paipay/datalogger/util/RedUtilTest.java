package ec.edu.espol.paipay.datalogger.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RedUtilTest {

    @Test
    public void produccionRequiereRedValidada() {
        assertFalse(RedUtil.esRedUtilizable(true, true, false, false));
        assertTrue(RedUtil.esRedUtilizable(true, true, true, false));
    }

    @Test
    public void backendLocalDebugAceptaRedNoValidadaDelAvd() {
        assertTrue(RedUtil.esRedUtilizable(true, true, false, true));
    }

    @Test
    public void backendLocalNoInventaTransporteNiInternet() {
        assertFalse(RedUtil.esRedUtilizable(false, true, false, true));
        assertFalse(RedUtil.esRedUtilizable(true, false, false, true));
    }
}
