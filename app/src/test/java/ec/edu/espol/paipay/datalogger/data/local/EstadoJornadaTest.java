package ec.edu.espol.paipay.datalogger.data.local;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;

public class EstadoJornadaTest {
    @Test public void soloLosCambiosSinEnviarEstanPendientes() {
        JornadaLocal jornada = new JornadaLocal();
        jornada.estadoLocal = JornadaLocal.PENDIENTE_CREAR;
        assertTrue(jornada.estaPendiente());
        jornada.estadoLocal = JornadaLocal.PENDIENTE_EDITAR;
        assertTrue(jornada.estaPendiente());
        jornada.estadoLocal = JornadaLocal.PENDIENTE_ANULAR;
        assertTrue(jornada.estaPendiente());
        jornada.estadoLocal = JornadaLocal.SINCRONIZADO;
        assertFalse(jornada.estaPendiente());
        jornada.estadoLocal = JornadaLocal.BORRADOR;
        assertFalse(jornada.estaPendiente());
    }
}
