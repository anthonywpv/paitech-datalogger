package ec.edu.espol.paipay.datalogger.domain;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ValidadorMovimientoTest {
    @Test public void siembraSoloAdmiteDestino() {
        assertNull(ValidadorMovimiento.validar("SIEMBRA", 20,
                null, "p2", null, "Tilapia"));
        assertNotNull(ValidadorMovimiento.validar("SIEMBRA", 20,
                "p1", "p2", "Tilapia", "Tilapia"));
    }

    @Test public void mortalidadSoloAdmiteOrigen() {
        assertNull(ValidadorMovimiento.validar("MORTALIDAD", 3,
                "p1", null, "Tilapia", null));
        assertNotNull(ValidadorMovimiento.validar("MORTALIDAD", 3,
                null, "p1", null, "Tilapia"));
    }

    @Test public void trasladoExigePiscinasDistintasDeLaMismaEspecie() {
        assertNull(ValidadorMovimiento.validar("TRASLADO", 10,
                "p1", "p2", "Tilapia", "Tilapia"));
        assertNotNull(ValidadorMovimiento.validar("TRASLADO", 10,
                "p1", "p1", "Tilapia", "Tilapia"));
        assertNotNull(ValidadorMovimiento.validar("TRASLADO", 10,
                "p1", "p2", "Tilapia", "Vieja azul"));
    }

    @Test public void ajusteExigeUnaSolaPiscina() {
        assertNull(ValidadorMovimiento.validar("AJUSTE", 2,
                "p1", null, "Tilapia", null));
        assertNull(ValidadorMovimiento.validar("AJUSTE", 2,
                null, "p1", null, "Tilapia"));
        assertNotNull(ValidadorMovimiento.validar("AJUSTE", 2,
                "p1", "p2", "Tilapia", "Tilapia"));
    }

    @Test public void cantidadDebeSerPositiva() {
        assertNotNull(ValidadorMovimiento.validar("SIEMBRA", 0,
                null, "p1", null, "Tilapia"));
    }
}
