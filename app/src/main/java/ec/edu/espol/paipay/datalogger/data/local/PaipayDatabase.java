package ec.edu.espol.paipay.datalogger.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Arrays;

import ec.edu.espol.paipay.datalogger.data.local.dao.AguaDao;
import ec.edu.espol.paipay.datalogger.data.local.dao.BiometriaDao;
import ec.edu.espol.paipay.datalogger.data.local.dao.LaboratorioDao;
import ec.edu.espol.paipay.datalogger.data.local.dao.PiscinaDao;
import ec.edu.espol.paipay.datalogger.data.local.entity.EnsayoLaboratorio;
import ec.edu.espol.paipay.datalogger.data.local.entity.Piscina;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;

/**
 * Base de datos LOCAL del teléfono (SQLite vía Room).
 *
 * Es el corazón de la "sincronización diferida": el productor registra siempre
 * contra esta base, tenga o no internet. La subida a Neon es un segundo paso,
 * explícito y controlado por el usuario.
 */
@Database(
        entities = {RegistroBiometria.class, RegistroAgua.class, EnsayoLaboratorio.class, Piscina.class},
        version = 2,
        exportSchema = true
)
public abstract class PaipayDatabase extends RoomDatabase {

    private static volatile PaipayDatabase INSTANCIA;

    /**
     * v1 → v2: la biometría pasa de "un promedio y un número de peces" a
     * "una fila por pez".
     *
     * SQLite no permite quitar una columna en las versiones que trae Android 7,
     * así que hay que recrear la tabla y copiar. NO se usa
     * fallbackToDestructiveMigration: borraría los registros que el productor
     * aún no ha sincronizado, que es justo lo que esta app promete no perder.
     *
     * Las filas viejas que promediaban varios peces se conservan como un único
     * registro —no hay forma de inventar las medidas individuales—, pero se les
     * anota en la observación de cuántos peces era el promedio, para que nadie
     * las lea después como si fueran una medición individual.
     */
    static final Migration MIGRACION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `registro_biometria_nueva` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`uuid` TEXT NOT NULL, `fecha_muestreo` TEXT NOT NULL, "
                    + "`piscina` TEXT NOT NULL, `peso_g` REAL NOT NULL, "
                    + "`talla_cm` REAL NOT NULL, `observacion` TEXT, "
                    + "`registrado_por` TEXT, `creado_en` INTEGER NOT NULL, "
                    + "`sincronizado` INTEGER NOT NULL, `sincronizado_en` INTEGER)");

            db.execSQL("INSERT INTO `registro_biometria_nueva` "
                    + "(id, uuid, fecha_muestreo, piscina, peso_g, talla_cm, observacion, "
                    + " registrado_por, creado_en, sincronizado, sincronizado_en) "
                    + "SELECT id, uuid, fecha_muestreo, piscina, peso_g, talla_cm, "
                    + "  CASE WHEN cantidad_muestreada > 1 "
                    + "       THEN COALESCE(observacion || ' | ', '') || 'Promedio de ' "
                    + "            || cantidad_muestreada || ' peces (registro anterior al "
                    + "cambio a medición individual).' "
                    + "       ELSE observacion END, "
                    + "  registrado_por, creado_en, sincronizado, sincronizado_en "
                    + "FROM `registro_biometria`");

            db.execSQL("DROP TABLE `registro_biometria`");
            db.execSQL("ALTER TABLE `registro_biometria_nueva` "
                    + "RENAME TO `registro_biometria`");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_registro_biometria_uuid` "
                    + "ON `registro_biometria` (`uuid`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_registro_biometria_sincronizado` "
                    + "ON `registro_biometria` (`sincronizado`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS "
                    + "`index_registro_biometria_fecha_muestreo_piscina` "
                    + "ON `registro_biometria` (`fecha_muestreo`, `piscina`)");
        }
    };

    public abstract BiometriaDao biometriaDao();
    public abstract AguaDao aguaDao();
    public abstract LaboratorioDao laboratorioDao();
    public abstract PiscinaDao piscinaDao();

    public static PaipayDatabase obtener(Context contexto) {
        if (INSTANCIA == null) {
            synchronized (PaipayDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(
                                    contexto.getApplicationContext(),
                                    PaipayDatabase.class,
                                    "paipay_datalogger.db")
                            .addCallback(SEMILLA)
                            .addMigrations(MIGRACION_1_2)
                            .build();
                }
            }
        }
        return INSTANCIA;
    }

    /** Carga el catálogo inicial de piscinas del recinto Paipayales. */
    private static final Callback SEMILLA = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            AppExecutors.io().execute(() -> {
                if (INSTANCIA == null) return;
                PiscinaDao dao = INSTANCIA.piscinaDao();
                if (dao.contar() == 0) {
                    dao.insertarTodas(Arrays.asList(
                            new Piscina("P-01", "Piscina 1 - Engorde", 120d),
                            new Piscina("P-02", "Piscina 2 - Engorde", 120d),
                            new Piscina("P-03", "Piscina 3 - Alevinaje", 60d),
                            new Piscina("P-04", "Piscina 4 - Reproductores", 80d),
                            new Piscina("UE-01", "U.E. Galo Plaza - Demostrativa", 40d),
                            new Piscina("LOM-01", "Lecho lombricultura 1", 12d),
                            new Piscina("LOM-02", "Lecho lombricultura 2", 12d)
                    ));
                }
            });
        }
    };
}
