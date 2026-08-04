package ec.edu.espol.paipay.datalogger.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
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
        version = 1,
        exportSchema = true
)
public abstract class PaipayDatabase extends RoomDatabase {

    private static volatile PaipayDatabase INSTANCIA;

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
