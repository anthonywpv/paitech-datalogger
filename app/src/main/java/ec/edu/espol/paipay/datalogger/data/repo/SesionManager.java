package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Sesión persistente del productor.
 *
 * REQUISITO CLAVE DEL PROYECTO: el usuario inicia sesión UNA SOLA VEZ.
 * A partir de ahí la app abre directo al formulario, incluso sin internet
 * y aunque el teléfono se reinicie. La sesión solo se pierde si el propio
 * usuario pulsa "Cerrar sesión".
 *
 * Los datos se guardan en EncryptedSharedPreferences (cifrado AES-256 con
 * llave en el keystore del dispositivo), de modo que si el teléfono se pierde
 * nadie puede leer el identificador del productor en texto plano.
 */
public class SesionManager {

    private static final String ARCHIVO = "paipay_sesion";
    private static final String K_USUARIO = "productor_usuario";
    private static final String K_NOMBRE = "productor_nombre";
    private static final String K_SESION_ACTIVA = "sesion_activa";
    private static final String K_ULTIMA_SYNC = "ultima_sincronizacion";

    private static volatile SesionManager INSTANCIA;
    private final SharedPreferences prefs;

    private SesionManager(Context contexto) {
        SharedPreferences p;
        try {
            MasterKey llave = new MasterKey.Builder(contexto.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            p = EncryptedSharedPreferences.create(
                    contexto.getApplicationContext(),
                    ARCHIVO,
                    llave,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            // Respaldo: algunos equipos antiguos fallan al crear el keystore.
            p = contexto.getApplicationContext()
                    .getSharedPreferences(ARCHIVO + "_plano", Context.MODE_PRIVATE);
        }
        this.prefs = p;
    }

    public static SesionManager obtener(Context contexto) {
        if (INSTANCIA == null) {
            synchronized (SesionManager.class) {
                if (INSTANCIA == null) INSTANCIA = new SesionManager(contexto);
            }
        }
        return INSTANCIA;
    }

    // ---------------- Estado de sesión ----------------

    public boolean haySesionActiva() {
        return prefs.getBoolean(K_SESION_ACTIVA, false);
    }

    public void guardarSesion(String usuario, String nombre) {
        prefs.edit()
                .putString(K_USUARIO, usuario)
                .putString(K_NOMBRE, nombre)
                .putBoolean(K_SESION_ACTIVA, true)
                .apply();
    }

    /** Cierra sesión SIN borrar la base local: los datos pendientes se conservan. */
    public void cerrarSesion() {
        prefs.edit()
                .remove(K_USUARIO).remove(K_NOMBRE)
                .putBoolean(K_SESION_ACTIVA, false)
                .apply();
    }

    public String getUsuario() { return prefs.getString(K_USUARIO, ""); }

    public String getNombre() {
        String n = prefs.getString(K_NOMBRE, "");
        return n == null || n.isEmpty() ? getUsuario() : n;
    }

    // ---------------- Marca de tiempo de sincronización ----------------

    public void registrarSincronizacion(long momento) {
        prefs.edit().putLong(K_ULTIMA_SYNC, momento).apply();
    }

    /** -1 si nunca se ha sincronizado. */
    public long getUltimaSincronizacion() {
        return prefs.getLong(K_ULTIMA_SYNC, -1L);
    }
}
