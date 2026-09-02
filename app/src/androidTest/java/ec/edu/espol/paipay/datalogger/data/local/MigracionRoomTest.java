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
    private static final String BASE_PRUEBA_CONFLICTOS = "migracion-conflictos";
    private static final String BASE_PRUEBA_AGUA = "migracion-agua-kit";
    private static final String BASE_PRUEBA_CICLOS = "migracion-ciclos-v15";
    private static final String BASE_PRUEBA_LOMBRICULTURA = "migracion-lombricultura-v16";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), PaipayDatabase.class);

    @Test public void migracionUnoADosConservaJornadaYCreaInstantaneas() throws Exception {
        SupportSQLiteDatabase db = helper.createDatabase(BASE_PRUEBA_CONFLICTOS, 1);
        db.execSQL("INSERT INTO jornada_local "
                + "(uuid,piscinaUuid,piscinaCodigo,autorCorreo,capturadaEn,incluyeAgua,"
                + "estadoLocal,versionServidor,creadaEn,modificadaEn) VALUES "
                + "('j-1','p-1','P-01','ana@example.com','2026-08-07T20:00:00Z',0,"
                + "'CONFLICTO',1,1,1)");
        db.close();

        db = helper.runMigrationsAndValidate(
                BASE_PRUEBA_CONFLICTOS, 2, true, PaipayDatabase.MIGRACION_1_2);
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

    @Test public void migracionDosATresConservaAguaYPecesYRenombraAmoniacoTotal()
            throws Exception {
        SupportSQLiteDatabase db = helper.createDatabase(BASE_PRUEBA_AGUA, 2);
        db.execSQL("INSERT INTO jornada_local "
                + "(uuid,piscinaUuid,piscinaCodigo,autorCorreo,capturadaEn,incluyeAgua,"
                + "ph,nitrato,nitrito,amonio,estadoLocal,versionServidor,creadaEn,modificadaEn) "
                + "VALUES ('j-kit','p-1','P-01','ana@example.com','2026-08-07T20:00:00Z',"
                + "1,7.2,10,0.25,0.25,'PENDIENTE_CREAR',0,1,1)");
        db.execSQL("INSERT INTO observacion_pez_local "
                + "(uuid,jornadaUuid,orden,pesoGramos,tallaCentimetros) "
                + "VALUES ('pez-1','j-kit',1,250,21)");
        db.execSQL("INSERT INTO semaforo_local "
                + "(piscinaUuid,piscinaCodigo,piscinaNombre,ph,nitrato,nitrito,amonio,"
                + "actualizadoEn) VALUES ('p-1','P-01','Piscina 1',7.2,10,0.25,0.25,1)");
        db.close();

        db = helper.runMigrationsAndValidate(
                BASE_PRUEBA_AGUA, 3, true, PaipayDatabase.MIGRACION_2_3);
        Cursor jornada = db.query("SELECT amoniacoTotal FROM jornada_local "
                + "WHERE uuid='j-kit'");
        jornada.moveToFirst();
        assertEquals(0.25, jornada.getDouble(0), 0.0001);
        jornada.close();
        Cursor peces = db.query("SELECT COUNT(*) FROM observacion_pez_local "
                + "WHERE jornadaUuid='j-kit'");
        peces.moveToFirst();
        assertEquals(1, peces.getInt(0));
        peces.close();
        Cursor semaforo = db.query("SELECT amoniacoTotal FROM semaforo_local "
                + "WHERE piscinaUuid='p-1'");
        semaforo.moveToFirst();
        assertEquals(0.25, semaforo.getDouble(0), 0.0001);
        semaforo.close();
        db.close();
    }

    @Test public void migracionTresACuatroConservaJornadasYPreparaCiclos() throws Exception {
        SupportSQLiteDatabase db = helper.createDatabase(BASE_PRUEBA_CICLOS, 3);
        db.execSQL("INSERT INTO jornada_local "
                + "(uuid,piscinaUuid,piscinaCodigo,autorCorreo,capturadaEn,incluyeAgua,"
                + "estadoLocal,versionServidor,creadaEn,modificadaEn) VALUES "
                + "('j-v15','p-1','P-01','ana@example.com','2026-08-07T20:00:00Z',0,"
                + "'SINCRONIZADO',1,1,1)");
        db.close();

        db = helper.runMigrationsAndValidate(
                BASE_PRUEBA_CICLOS, 4, true, PaipayDatabase.MIGRACION_3_4);
        Cursor jornadas = db.query("SELECT cicloUuid FROM jornada_local WHERE uuid='j-v15'");
        jornadas.moveToFirst();
        assertEquals(true, jornadas.isNull(0));
        jornadas.close();
        Cursor ciclos = db.query("SELECT COUNT(*) FROM ciclo_local");
        ciclos.moveToFirst();
        assertEquals(0, ciclos.getInt(0));
        ciclos.close();
        db.close();
    }

    @Test public void migracionCuatroACincoAgregaCamasSinAlterarAcuicultura()
            throws Exception {
        SupportSQLiteDatabase db = helper.createDatabase(BASE_PRUEBA_LOMBRICULTURA, 4);
        db.execSQL("INSERT INTO piscina_local "
                + "(uuid,codigo,nombre,tipo,activa) VALUES "
                + "('p-v16','P-01','Piscina conservada','PECES',1)");
        db.close();

        db = helper.runMigrationsAndValidate(
                BASE_PRUEBA_LOMBRICULTURA, 5, true, PaipayDatabase.MIGRACION_4_5);
        Cursor piscinas = db.query("SELECT COUNT(*) FROM piscina_local WHERE uuid='p-v16'");
        piscinas.moveToFirst();
        assertEquals(1, piscinas.getInt(0));
        piscinas.close();
        Cursor camas = db.query("SELECT COUNT(*) FROM cama_local");
        camas.moveToFirst();
        assertEquals(0, camas.getInt(0));
        camas.close();
        Cursor ciclos = db.query("SELECT COUNT(*) FROM ciclo_lombricultura_local");
        ciclos.moveToFirst();
        assertEquals(0, ciclos.getInt(0));
        ciclos.close();
        db.close();
    }
}
