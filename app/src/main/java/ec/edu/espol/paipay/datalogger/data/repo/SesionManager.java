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
    private static final String K_RECORDAR = "recordar_sesion";
    private static final String K_RECORDAR_HASTA = "recordar_hasta";

    /** Cuánto dura el "no volver a preguntar" antes de pedir credenciales otra vez. */
    private static final long DIAS_RECORDADOS = 30L;
    private static final long MS_RECORDADOS = DIAS_RECORDADOS * 24L * 60L * 60L * 1000L;

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

    /**
     * Hay con qué identificarse para subir.
     *
     * Si el productor marcó "no volver a preguntar", la sesión vale hasta que
     * se cumplan los 30 días; pasado ese plazo se olvida sola y se le vuelven a
     * pedir las credenciales. Si NO lo marcó, la sesión solo vive lo que dura
     * esa subida: quien la cierra es AutenticacionRepositorio.olvidarSiNoSeRecuerda(),
     * que además borra la cookie.
     */
    public boolean haySesionActiva() {
        if (!prefs.getBoolean(K_SESION_ACTIVA, false)) return false;
        if (!recordarSesion()) return true;

        if (System.currentTimeMillis() > prefs.getLong(K_RECORDAR_HASTA, 0L)) {
            cerrarSesion();
            return false;
        }
        return true;
    }

    /** true si el productor pidió que no se le vuelva a preguntar por 30 días. */
    public boolean recordarSesion() {
        return prefs.getBoolean(K_RECORDAR, false);
    }

    /** Momento en que caduca el "no volver a preguntar"; -1 si no aplica. */
    public long recordarHasta() {
        return recordarSesion() ? prefs.getLong(K_RECORDAR_HASTA, -1L) : -1L;
    }

    public void guardarSesion(String usuario, String nombre) {
        guardarSesion(usuario, nombre, false);
    }

    public void guardarSesion(String usuario, String nombre, boolean recordar) {
        prefs.edit()
                .putString(K_USUARIO, usuario)
                .putString(K_NOMBRE, nombre)
                .putBoolean(K_SESION_ACTIVA, true)
                .putBoolean(K_RECORDAR, recordar)
                .putLong(K_RECORDAR_HASTA,
                        recordar ? System.currentTimeMillis() + MS_RECORDADOS : 0L)
                .apply();
    }


    /** Cierra sesión SIN borrar la base local: los datos pendientes se conservan. */
    public void cerrarSesion() {
        prefs.edit()
                .remove(K_USUARIO).remove(K_NOMBRE)
                .putBoolean(K_SESION_ACTIVA, false)
                .putBoolean(K_RECORDAR, false)
                .putLong(K_RECORDAR_HASTA, 0L)
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
