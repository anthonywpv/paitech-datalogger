package ec.edu.espol.paipay.datalogger.data.local;

import static org.junit.Assert.assertEquals;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MigracionRoomTest {
    private static final String BASE_PRUEBA = "migracion-conflictos";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), PaipayDatabase.class);

    @Test public void migracionUnoADosConservaJornadaYCreaInstantaneas() throws Exception {
        SupportSQLiteDatabase db = helper.createDatabase(BASE_PRUEBA, 1);
        db.execSQL("INSERT INTO jornada_local "
                + "(uuid,piscinaUuid,piscinaCodigo,autorCorreo,capturadaEn,incluyeAgua,"
                + "estadoLocal,versionServidor,creadaEn,modificadaEn) VALUES "
                + "('j-1','p-1','P-01','ana@example.com','2026-08-07T20:00:00Z',0,"
                + "'CONFLICTO',1,1,1)");
        db.close();

        db = helper.runMigrationsAndValidate(
                BASE_PRUEBA, 2, true, PaipayDatabase.MIGRACION_1_2);
        Cursor jornadas = db.query("SELECT COUNT(*) FROM jornada_local WHERE uuid='j-1'");
        jornadas.moveToFirst();
        assertEquals(1, jornadas.getInt(0));
        jornadas.close();

        db.execSQL("INSERT INTO conflicto_local "
                + "(clave,tipo,entidadUuid,operacionLocal,versionRemota,remotoJson,detectadoEn) "
                + "VALUES ('JORNADA:j-1','JORNADA','j-1','PENDIENTE_EDITAR',2,'{}',2)");
        Cursor conflictos = db.query("SELECT versionRemota FROM conflicto_local "
                + "WHERE clave='JORNADA:j-1'");
        conflictos.moveToFirst();
        assertEquals(2, conflictos.getInt(0));
        conflictos.close();
        db.close();
    }
}
