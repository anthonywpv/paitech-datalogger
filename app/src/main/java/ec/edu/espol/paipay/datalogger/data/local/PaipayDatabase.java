package ec.edu.espol.paipay.datalogger.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import ec.edu.espol.paipay.datalogger.data.local.dao.CatalogoDao;
import ec.edu.espol.paipay.datalogger.data.local.dao.JornadaDao;
import ec.edu.espol.paipay.datalogger.data.local.dao.MovimientoDao;
import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.SemaforoLocal;

/** Base offline nueva de v1.4. El archivo v1.3 no se abre ni se modifica. */
@Database(
        entities = {JornadaLocal.class, ObservacionPezLocal.class, PiscinaLocal.class,
                SemaforoLocal.class, MovimientoLocal.class},
        version = 1,
        exportSchema = true
)
public abstract class PaipayDatabase extends RoomDatabase {
    public static final String ARCHIVO = "paipay_datalogger_v14.db";
    private static volatile PaipayDatabase INSTANCIA;

    public abstract JornadaDao jornadaDao();
    public abstract CatalogoDao catalogoDao();
    public abstract MovimientoDao movimientoDao();

    public static PaipayDatabase obtener(Context contexto) {
        if (INSTANCIA == null) {
            synchronized (PaipayDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(
                            contexto.getApplicationContext(), PaipayDatabase.class, ARCHIVO
                    ).build();
                }
            }
        }
        return INSTANCIA;
    }
}
