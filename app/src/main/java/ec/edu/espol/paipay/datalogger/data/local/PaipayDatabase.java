package ec.edu.espol.paipay.datalogger.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import ec.edu.espol.paipay.datalogger.data.local.dao.CatalogoDao;
import ec.edu.espol.paipay.datalogger.data.local.dao.ConflictoDao;
import ec.edu.espol.paipay.datalogger.data.local.dao.JornadaDao;
import ec.edu.espol.paipay.datalogger.data.local.dao.MovimientoDao;
import ec.edu.espol.paipay.datalogger.data.local.entity.ConflictoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.SemaforoLocal;

/** Base offline nueva de v1.4. El archivo v1.3 no se abre ni se modifica. */
@Database(
        entities = {JornadaLocal.class, ObservacionPezLocal.class, PiscinaLocal.class,
                SemaforoLocal.class, MovimientoLocal.class, ConflictoLocal.class},
        version = 3,
        exportSchema = true
)
public abstract class PaipayDatabase extends RoomDatabase {
    public static final String ARCHIVO = "paipay_datalogger_v14.db";
    private static volatile PaipayDatabase INSTANCIA;

    public abstract JornadaDao jornadaDao();
    public abstract CatalogoDao catalogoDao();
    public abstract MovimientoDao movimientoDao();
    public abstract ConflictoDao conflictoDao();

    public static final Migration MIGRACION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `conflicto_local` "
                    + "(`clave` TEXT NOT NULL, `tipo` TEXT NOT NULL, "
                    + "`entidadUuid` TEXT NOT NULL, `operacionLocal` TEXT NOT NULL, "
                    + "`versionRemota` INTEGER NOT NULL, `remotoJson` TEXT, "
                    + "`detectadoEn` INTEGER NOT NULL, PRIMARY KEY(`clave`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_conflicto_local_entidadUuid` "
                    + "ON `conflicto_local` (`entidadUuid`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_conflicto_local_tipo` "
                    + "ON `conflicto_local` (`tipo`)");
        }
    };

    /** Renombra amonio a amoníaco total sin depender de RENAME COLUMN (API 24). */
    public static final Migration MIGRACION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE `jornada_local_nueva` ("
                    + "`uuid` TEXT NOT NULL, `piscinaUuid` TEXT NOT NULL, "
                    + "`piscinaCodigo` TEXT NOT NULL, `especieNombre` TEXT, "
                    + "`autorCorreo` TEXT NOT NULL, `capturadaEn` TEXT NOT NULL, "
                    + "`poblacionEstimada` INTEGER, `observaciones` TEXT, "
                    + "`incluyeAgua` INTEGER NOT NULL, `ph` REAL, `nitrato` REAL, "
                    + "`nitrito` REAL, `amoniacoTotal` REAL, `estadoLocal` TEXT NOT NULL, "
                    + "`versionServidor` INTEGER NOT NULL, `motivoCambio` TEXT, "
                    + "`errorSincronizacion` TEXT, `creadaEn` INTEGER NOT NULL, "
                    + "`modificadaEn` INTEGER NOT NULL, PRIMARY KEY(`uuid`))");
            db.execSQL("INSERT INTO `jornada_local_nueva` ("
                    + "uuid,piscinaUuid,piscinaCodigo,especieNombre,autorCorreo,capturadaEn,"
                    + "poblacionEstimada,observaciones,incluyeAgua,ph,nitrato,nitrito,"
                    + "amoniacoTotal,estadoLocal,versionServidor,motivoCambio,"
                    + "errorSincronizacion,creadaEn,modificadaEn) SELECT "
                    + "uuid,piscinaUuid,piscinaCodigo,especieNombre,autorCorreo,capturadaEn,"
                    + "poblacionEstimada,observaciones,incluyeAgua,ph,nitrato,nitrito,"
                    + "amonio,estadoLocal,versionServidor,motivoCambio,errorSincronizacion,"
                    + "creadaEn,modificadaEn FROM `jornada_local`");

            // Se respalda la tabla hija antes de reemplazar su tabla padre.
            db.execSQL("CREATE TABLE `observacion_pez_local_respaldo` ("
                    + "`uuid` TEXT NOT NULL, `jornadaUuid` TEXT NOT NULL, "
                    + "`orden` INTEGER NOT NULL, `pesoGramos` REAL NOT NULL, "
                    + "`tallaCentimetros` REAL NOT NULL, PRIMARY KEY(`uuid`))");
            db.execSQL("INSERT INTO `observacion_pez_local_respaldo` SELECT "
                    + "uuid,jornadaUuid,orden,pesoGramos,tallaCentimetros "
                    + "FROM `observacion_pez_local`");
            db.execSQL("DROP TABLE `observacion_pez_local`");
            db.execSQL("DROP TABLE `jornada_local`");
            db.execSQL("ALTER TABLE `jornada_local_nueva` RENAME TO `jornada_local`");

            db.execSQL("CREATE TABLE `observacion_pez_local_nueva` ("
                    + "`uuid` TEXT NOT NULL, `jornadaUuid` TEXT NOT NULL, "
                    + "`orden` INTEGER NOT NULL, `pesoGramos` REAL NOT NULL, "
                    + "`tallaCentimetros` REAL NOT NULL, PRIMARY KEY(`uuid`), "
                    + "FOREIGN KEY(`jornadaUuid`) REFERENCES `jornada_local`(`uuid`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("INSERT INTO `observacion_pez_local_nueva` SELECT "
                    + "uuid,jornadaUuid,orden,pesoGramos,tallaCentimetros "
                    + "FROM `observacion_pez_local_respaldo`");
            db.execSQL("DROP TABLE `observacion_pez_local_respaldo`");
            db.execSQL("ALTER TABLE `observacion_pez_local_nueva` "
                    + "RENAME TO `observacion_pez_local`");
            db.execSQL("CREATE INDEX `index_jornada_local_autorCorreo` "
                    + "ON `jornada_local` (`autorCorreo`)");
            db.execSQL("CREATE INDEX `index_jornada_local_piscinaUuid` "
                    + "ON `jornada_local` (`piscinaUuid`)");
            db.execSQL("CREATE INDEX `index_jornada_local_capturadaEn` "
                    + "ON `jornada_local` (`capturadaEn`)");
            db.execSQL("CREATE INDEX `index_jornada_local_estadoLocal` "
                    + "ON `jornada_local` (`estadoLocal`)");
            db.execSQL("CREATE INDEX `index_observacion_pez_local_jornadaUuid` "
                    + "ON `observacion_pez_local` (`jornadaUuid`)");
            db.execSQL("CREATE UNIQUE INDEX `index_observacion_pez_local_jornadaUuid_orden` "
                    + "ON `observacion_pez_local` (`jornadaUuid`, `orden`)");

            db.execSQL("CREATE TABLE `semaforo_local_nuevo` ("
                    + "`piscinaUuid` TEXT NOT NULL, `piscinaCodigo` TEXT NOT NULL, "
                    + "`piscinaNombre` TEXT NOT NULL, `jornadaUuid` TEXT, "
                    + "`capturadaEn` TEXT, `autorNombre` TEXT, `ph` REAL, "
                    + "`nitrato` REAL, `nitrito` REAL, `amoniacoTotal` REAL, "
                    + "`estado` TEXT, `resumen` TEXT, `actualizadoEn` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`piscinaUuid`))");
            db.execSQL("INSERT INTO `semaforo_local_nuevo` ("
                    + "piscinaUuid,piscinaCodigo,piscinaNombre,jornadaUuid,capturadaEn,"
                    + "autorNombre,ph,nitrato,nitrito,amoniacoTotal,estado,resumen,actualizadoEn) "
                    + "SELECT piscinaUuid,piscinaCodigo,piscinaNombre,jornadaUuid,capturadaEn,"
                    + "autorNombre,ph,nitrato,nitrito,amonio,estado,resumen,actualizadoEn "
                    + "FROM `semaforo_local`");
            db.execSQL("DROP TABLE `semaforo_local`");
            db.execSQL("ALTER TABLE `semaforo_local_nuevo` RENAME TO `semaforo_local`");
        }
    };

    public static PaipayDatabase obtener(Context contexto) {
        if (INSTANCIA == null) {
            synchronized (PaipayDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(
                            contexto.getApplicationContext(), PaipayDatabase.class, ARCHIVO
                    ).addMigrations(MIGRACION_1_2, MIGRACION_2_3).build();
                }
            }
        }
        return INSTANCIA;
    }
}
