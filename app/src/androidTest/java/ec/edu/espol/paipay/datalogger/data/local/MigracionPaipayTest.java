package ec.edu.espol.paipay.datalogger.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Prueba de la migración 1 → 2 sobre un dispositivo real.
 *
 * Es la prueba que de verdad importa de este cambio: la migración recrea las dos
 * tablas principales, y si el esquema resultante no coincide EXACTAMENTE con el
 * que Room espera, la app revienta al abrir en el teléfono de un productor que
 * lleva datos sin sincronizar encima.
 *
 * runMigrationsAndValidate hace justo esa comprobación: ejecuta la migración y
 * después valida columna por columna e índice por índice contra el esquema
 * exportado en app/schemas/…/2.json. Comparar el DDL a ojo no da esa garantía.
 */
@RunWith(AndroidJUnit4.class)
public class MigracionPaipayTest {

    private static final String BD = "prueba-migracion.db";

    @Rule
    public MigrationTestHelper ayudante = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), PaipayDatabase.class);

    /** Deja una base v1 con datos, incluidos registros SIN sincronizar. */
    private void sembrarV1() throws IOException {
        SupportSQLiteDatabase db = ayudante.createDatabase(BD, 1);

        // Biometría: una fila pendiente que promediaba 20 peces y una ya subida.
        db.execSQL("INSERT INTO registro_biometria (uuid, fecha_muestreo, piscina, peso_g, "
                + "talla_cm, cantidad_muestreada, observacion, registrado_por, creado_en, "
                + "sincronizado, sincronizado_en) VALUES "
                + "('u-1','2026-08-01','P-01',150.5,12.8,20,'Muestreo general',"
                + "'productor@paipayales.ec',1754000000000,0,NULL)");
        db.execSQL("INSERT INTO registro_biometria (uuid, fecha_muestreo, piscina, peso_g, "
                + "talla_cm, cantidad_muestreada, observacion, registrado_por, creado_en, "
                + "sincronizado, sincronizado_en) VALUES "
                + "('u-2','2026-08-02','P-02',145.0,12.3,1,NULL,"
                + "'productor@paipayales.ec',1754100000000,1,1754200000000)");

        // Agua: una pendiente SIN pH (era opcional) y otra con pH.
        db.execSQL("INSERT INTO registro_agua (uuid, fecha_muestreo, piscina, temperatura_c, "
                + "oxigeno_mg_l, ph, observacion, registrado_por, creado_en, sincronizado, "
                + "sincronizado_en) VALUES "
                + "('a-1','2026-08-01','P-01',31.0,4.5,NULL,NULL,"
                + "'productor@paipayales.ec',1754000000000,0,NULL)");
        db.execSQL("INSERT INTO registro_agua (uuid, fecha_muestreo, piscina, temperatura_c, "
                + "oxigeno_mg_l, ph, observacion, registrado_por, creado_en, sincronizado, "
                + "sincronizado_en) VALUES "
                + "('a-2','2026-08-02','P-02',28.5,6.2,7.4,'Agua turbia',"
                + "'productor@paipayales.ec',1754100000000,1,1754200000000)");

        db.close();
    }

    /** Ejecuta la migración y valida el esquema resultante contra 2.json. */
    private SupportSQLiteDatabase migrar() throws IOException {
        return ayudante.runMigrationsAndValidate(
                BD, 2, true, PaipayDatabase.MIGRACION_1_2);
    }

    // ------------------------------------------------------------------

    /**
     * El caso que hace fallar la app al arrancar si el DDL no cuadra.
     * runMigrationsAndValidate lanza excepción si algo no coincide.
     */
    @Test
    public void migracion1a2_dejaElEsquemaQueRoomEspera() throws IOException {
        sembrarV1();
        migrar().close();
    }

    @Test
    public void migracion1a2_noPierdeNingunRegistro() throws IOException {
        sembrarV1();
        SupportSQLiteDatabase db = migrar();

        assertEquals(2, contar(db, "SELECT COUNT(*) FROM registro_biometria"));
        assertEquals(2, contar(db, "SELECT COUNT(*) FROM registro_agua"));
        db.close();
    }

    /** Lo que la app promete: un dato sin sincronizar no se pierde nunca. */
    @Test
    public void migracion1a2_conservaLosPendientes() throws IOException {
        sembrarV1();
        SupportSQLiteDatabase db = migrar();

        assertEquals("el pez pendiente sigue pendiente",
                1, contar(db, "SELECT COUNT(*) FROM registro_biometria "
                        + "WHERE uuid='u-1' AND sincronizado=0"));
        assertEquals("la medición de agua pendiente sigue pendiente",
                1, contar(db, "SELECT COUNT(*) FROM registro_agua "
                        + "WHERE uuid='a-1' AND sincronizado=0"));
        db.close();
    }

    @Test
    public void migracion1a2_retiraLasColumnasViejas() throws IOException {
        sembrarV1();
        SupportSQLiteDatabase db = migrar();

        List<String> biometria = columnas(db, "registro_biometria");
        assertFalse(biometria.contains("cantidad_muestreada"));

        List<String> agua = columnas(db, "registro_agua");
        assertFalse(agua.contains("temperatura_c"));
        assertFalse(agua.contains("oxigeno_mg_l"));
        assertTrue(agua.contains("amonio_mg_l"));
        assertTrue(agua.contains("nitrito_mg_l"));
        assertTrue(agua.contains("nitrato_mg_l"));
        assertTrue(agua.contains("poblacion_estimada"));
        db.close();
    }

    /**
     * Una fila que promediaba 20 peces no se puede desagregar, pero tampoco debe
     * quedar pasando por la medida de un solo pez.
     */
    @Test
    public void migracion1a2_anotaLasFilasQuePromediabanVariosPeces() throws IOException {
        sembrarV1();
        SupportSQLiteDatabase db = migrar();

        String obs = texto(db, "SELECT observacion FROM registro_biometria WHERE uuid='u-1'");
        assertTrue("debe conservar la observación original", obs.contains("Muestreo general"));
        assertTrue("y avisar de que era un promedio", obs.contains("Promedio de 20 peces"));

        // La que ya era de un solo pez no se toca.
        assertEquals("", texto(db, "SELECT COALESCE(observacion,'') "
                + "FROM registro_biometria WHERE uuid='u-2'"));
        db.close();
    }

    /** Las lecturas viejas de temperatura y oxígeno no se tiran: se anotan. */
    @Test
    public void migracion1a2_conservaLasLecturasViejasDeAgua() throws IOException {
        sembrarV1();
        SupportSQLiteDatabase db = migrar();

        String obs = texto(db, "SELECT observacion FROM registro_agua WHERE uuid='a-2'");
        assertTrue(obs.contains("Agua turbia"));
        assertTrue("debe dejar constancia de la temperatura", obs.contains("28.5"));
        assertTrue("y del oxígeno", obs.contains("6.2"));
        db.close();
    }

    /** El pH pasa de opcional a obligatorio: donde faltaba se asume neutro. */
    @Test
    public void migracion1a2_rellenaElPhQueFaltaba() throws IOException {
        sembrarV1();
        SupportSQLiteDatabase db = migrar();

        assertEquals(7.0, doble(db, "SELECT ph FROM registro_agua WHERE uuid='a-1'"), 0.001);
        assertEquals("el pH ya anotado se respeta",
                7.4, doble(db, "SELECT ph FROM registro_agua WHERE uuid='a-2'"), 0.001);
        db.close();
    }

    // ------------------------------------------------------------------

    private static int contar(SupportSQLiteDatabase db, String sql) {
        try (Cursor c = db.query(sql)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    private static String texto(SupportSQLiteDatabase db, String sql) {
        try (Cursor c = db.query(sql)) {
            c.moveToFirst();
            return c.isNull(0) ? "" : c.getString(0);
        }
    }

    private static double doble(SupportSQLiteDatabase db, String sql) {
        try (Cursor c = db.query(sql)) {
            c.moveToFirst();
            return c.getDouble(0);
        }
    }

    private static List<String> columnas(SupportSQLiteDatabase db, String tabla) {
        List<String> nombres = new ArrayList<>();
        try (Cursor c = db.query("PRAGMA table_info(" + tabla + ")")) {
            int idx = c.getColumnIndex("name");
            while (c.moveToNext()) nombres.add(c.getString(idx));
        }
        return nombres;
    }
}
