package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;
import android.content.SharedPreferences;

import ec.edu.espol.paipay.datalogger.util.SeguridadUtil;

/** Identificador aleatorio de esta instalación; no depende de hardware ni cuenta. */
public final class DispositivoManager {
    private static final String ARCHIVO = "paipay_dispositivo_v14";
    private static final String K_ID = "id_instalacion";

    private DispositivoManager() { }

    public static synchronized String obtener(Context contexto) {
        SharedPreferences preferencias = contexto.getApplicationContext()
                .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
        String id = preferencias.getString(K_ID, null);
        if (id == null || id.isEmpty()) {
            id = SeguridadUtil.nuevoUuid();
            preferencias.edit().putString(K_ID, id).apply();
        }
        return "android-" + id;
    }
}
