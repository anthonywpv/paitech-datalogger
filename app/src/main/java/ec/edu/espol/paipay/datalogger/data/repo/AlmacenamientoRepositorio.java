package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;

import java.io.File;
import java.util.Locale;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;

/**
 * Cuánto ocupan los datos en el teléfono y cómo liberar espacio.
 *
 * REGLA INNEGOCIABLE: solo se borra lo que YA ESTÁ en la base principal. Un
 * registro pendiente es trabajo del productor que todavía no existe en ningún
 * otro sitio; perderlo sería el peor fallo posible de esta app. Por eso el
 * filtro `sincronizado = 1` vive en la propia consulta SQL y no en un `if`
 * que alguien pueda saltarse más adelante.
 */
public class AlmacenamientoRepositorio {

    /** Foto del espacio ocupado y de qué se puede liberar. */
    public static class Resumen {
        public final int sincronizados;
        public final int pendientes;
        /** Bytes que ocupa la base local, incluidos sus archivos auxiliares. */
        public final long bytes;

        Resumen(int sincronizados, int pendientes, long bytes) {
            this.sincronizados = sincronizados;
            this.pendientes = pendientes;
            this.bytes = bytes;
        }

        public int total() {
            return sincronizados + pendientes;
        }

        public boolean hayQueLiberar() {
            return sincronizados > 0;
        }

        /** "1,4 MB" o "812 kB", lo que le dice algo a una persona. */
        public String tamanoLegible() {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) {
                return String.format(Locale.getDefault(), "%d kB", bytes / 1024);
            }
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f));
        }
    }

    public interface AlResumir {
        void listo(Resumen resumen);
    }

    public interface AlLiberar {
        void listo(int borrados);
    }

    private final Context contexto;
    private final PaipayDatabase db;

    public AlmacenamientoRepositorio(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.db = PaipayDatabase.obtener(contexto);
    }

    public void resumir(AlResumir callback) {
        AppExecutors.io().execute(() -> {
            int sincronizados = db.biometriaDao().contarSincronizados()
                    + db.aguaDao().contarSincronizados()
                    + db.laboratorioDao().contarSincronizados();
            int pendientes = db.biometriaDao().contarPendientes()
                    + db.aguaDao().contarPendientes()
                    + db.laboratorioDao().contarPendientes();

            Resumen resumen = new Resumen(sincronizados, pendientes, bytesDeLaBase());
            AppExecutors.enHiloPrincipal(() -> callback.listo(resumen));
        });
    }

    /** Borra los registros ya subidos. Los pendientes no se tocan. */
    public void liberarSincronizados(AlLiberar callback) {
        AppExecutors.io().execute(() -> {
            int borrados = db.biometriaDao().borrarSincronizados()
                    + db.aguaDao().borrarSincronizados()
                    + db.laboratorioDao().borrarSincronizados();

            // VACUUM devuelve al sistema el espacio que SQLite dejó reservado;
            // sin esto el archivo no encoge y el productor no vería diferencia.
            try {
                db.getOpenHelper().getWritableDatabase().query("VACUUM").close();
            } catch (Exception ignorada) {
                // Si falla, los datos ya se borraron: solo no se compacta el archivo.
            }

            final int total = borrados;
            AppExecutors.enHiloPrincipal(() -> callback.listo(total));
        });
    }

    /**
     * La base son tres archivos: el principal, el -wal y el -shm. Contar solo
     * el primero daría una cifra que no se parece a lo que ocupa de verdad.
     */
    private long bytesDeLaBase() {
        long total = 0;
        for (String sufijo : new String[]{"", "-wal", "-shm"}) {
            File f = contexto.getDatabasePath("paipay_datalogger.db" + sufijo);
            if (f != null && f.exists()) total += f.length();
        }
        return total;
    }
}
