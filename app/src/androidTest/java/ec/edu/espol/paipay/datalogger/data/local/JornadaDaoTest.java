package ec.edu.espol.paipay.datalogger.data.local;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;

@RunWith(AndroidJUnit4.class)
public class JornadaDaoTest {
    private PaipayDatabase db;

    @Before public void preparar() {
        Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();
        db = Room.inMemoryDatabaseBuilder(contexto, PaipayDatabase.class)
                .allowMainThreadQueries().build();
    }

    @After public void cerrar() { db.close(); }

    @Test public void jornadaYPececilloSeGuardanComoUnaUnidad() {
        JornadaLocal jornada = new JornadaLocal();
        jornada.uuid = "j-1";
        jornada.piscinaUuid = "p-1";
        jornada.piscinaCodigo = "P-01";
        jornada.autorCorreo = "ana@example.com";
        jornada.capturadaEn = "2026-08-07T15:00:00-05:00";
        jornada.poblacionEstimada = 200;
        jornada.estadoLocal = JornadaLocal.PENDIENTE_CREAR;

        ObservacionPezLocal pez = new ObservacionPezLocal();
        pez.uuid = "pez-1";
        pez.jornadaUuid = jornada.uuid;
        pez.orden = 1;
        pez.pesoGramos = 250.4;
        pez.tallaCentimetros = 21.3;
        db.jornadaDao().guardar(jornada, Collections.singletonList(pez));

        JornadaConPeces guardada = db.jornadaDao().porUuid("j-1");
        assertEquals(200, (int) guardada.jornada.poblacionEstimada);
        assertEquals(1, guardada.peces.size());
        assertEquals(250.4, guardada.peces.get(0).pesoGramos, 0.001);
    }
}
