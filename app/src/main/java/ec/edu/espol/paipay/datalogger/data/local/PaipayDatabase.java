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
        version = 2,
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

    public static PaipayDatabase obtener(Context contexto) {
        if (INSTANCIA == null) {
            synchronized (PaipayDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(
                            contexto.getApplicationContext(), PaipayDatabase.class, ARCHIVO
                    ).addMigrations(MIGRACION_1_2).build();
                }
            }
        }
        return INSTANCIA;
    }
}
