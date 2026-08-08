package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;
import android.os.StatFs;

import java.io.File;
import java.util.Locale;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;

public class AlmacenamientoRepositorio {
    public static final long UMBRAL_AVISO_BYTES = 100L * 1024L * 1024L;
    public static final long UMBRAL_CRITICO_BYTES = 5L * 1024L * 1024L;

    public static class Resumen {
        public final int sincronizados;
        public final int pendientes;
        public final long bytes;
        public final long disponibles;
        Resumen(int sincronizados, int pendientes, long bytes, long disponibles) {
            this.sincronizados = sincronizados;
            this.pendientes = pendientes;
            this.bytes = bytes;
            this.disponibles = disponibles;
        }
        public int total() { return sincronizados + pendientes; }
        public boolean hayQueLiberar() { return sincronizados > 0; }
        public boolean espacioBajo() { return disponibles < UMBRAL_AVISO_BYTES; }
        public String tamanoLegible() { return legible(bytes); }
        public String disponibleLegible() { return legible(disponibles); }
    }

    public interface AlResumir { void listo(Resumen resumen); }
    public interface AlLiberar { void listo(int borrados); }

    private final Context contexto;
    private final PaipayDatabase db;

    public AlmacenamientoRepositorio(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        db = PaipayDatabase.obtener(contexto);
    }

    public boolean espacioCritico() {
        return bytesDisponibles() < UMBRAL_CRITICO_BYTES;
    }

    public void resumir(AlResumir callback) {
        AppExecutors.io().execute(() -> {
            int sincronizados = db.jornadaDao().contarSincronizados()
                    + db.movimientoDao().contarSincronizados();
            int pendientes = db.jornadaDao().contarNoResueltas()
                    + db.movimientoDao().contarNoResueltos();
            Resumen resumen = new Resumen(sincronizados, pendientes,
                    bytesDeLaBase(), bytesDisponibles());
            AppExecutors.enHiloPrincipal(() -> callback.listo(resumen));
        });
    }

    public void liberarSincronizados(AlLiberar callback) {
        AppExecutors.io().execute(() -> {
            int borrados = db.jornadaDao().borrarSincronizados()
                    + db.movimientoDao().borrarSincronizados();
            try {
                db.getOpenHelper().getWritableDatabase().query("VACUUM").close();
            } catch (Exception ignorada) { }
            AppExecutors.enHiloPrincipal(() -> callback.listo(borrados));
        });
    }

    private long bytesDisponibles() {
        try {
            return new StatFs(contexto.getFilesDir().getAbsolutePath()).getAvailableBytes();
        } catch (Exception error) {
            return Long.MAX_VALUE;
        }
    }

    private long bytesDeLaBase() {
        long total = 0;
        for (String sufijo : new String[]{"", "-wal", "-shm"}) {
            File archivo = contexto.getDatabasePath(PaipayDatabase.ARCHIVO + sufijo);
            if (archivo != null && archivo.exists()) total += archivo.length();
        }
        return total;
    }

    private static String legible(long bytes) {
        if (bytes == Long.MAX_VALUE) return "desconocido";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%d kB", bytes / 1024);
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f));
    }
}
