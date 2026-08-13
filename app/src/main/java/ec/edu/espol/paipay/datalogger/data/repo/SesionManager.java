package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/** Conserva cifrados el token Django y la identidad que firma los datos offline. */
public class SesionManager {
    private static final String ARCHIVO = "paipay_sesion_v14";
    private static final String K_CORREO = "correo";
    private static final String K_NOMBRE = "nombre";
    private static final String K_TOKEN = "token";
    private static final String K_ULTIMA_SYNC = "ultima_sync";
    private static final String K_ULTIMA_VALIDACION = "ultima_validacion_servidor";
    private static final String K_PROPIETARIO_LOCAL = "propietario_datos_locales";
    private static final String K_CAMBIO_CLAVE_REQUERIDO = "cambio_clave_requerido";
    public static final long VIGENCIA_OFFLINE_MS = 30L * 24L * 60L * 60L * 1000L;
    private static volatile SesionManager INSTANCIA;
    private final SharedPreferences prefs;

    private SesionManager(Context contexto) {
        SharedPreferences p;
        try {
            MasterKey llave = new MasterKey.Builder(contexto.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            p = EncryptedSharedPreferences.create(
                    contexto.getApplicationContext(), ARCHIVO, llave,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception error) {
            throw new IllegalStateException(
                    "No se pudo inicializar el almacenamiento cifrado de la sesión.", error);
        }
        prefs = p;
    }

    public static SesionManager obtener(Context contexto) {
        if (INSTANCIA == null) {
            synchronized (SesionManager.class) {
                if (INSTANCIA == null) INSTANCIA = new SesionManager(contexto);
            }
        }
        return INSTANCIA;
    }

    public void guardarSesion(String correo, String nombre, String token) {
        guardarSesion(correo, nombre, token, false);
    }

    public void guardarSesion(String correo, String nombre, String token,
                              boolean cambioClaveRequerido) {
        prefs.edit().putString(K_CORREO, correo).putString(K_NOMBRE, nombre)
                .putString(K_TOKEN, token)
                .putString(K_PROPIETARIO_LOCAL, correo)
                .putBoolean(K_CAMBIO_CLAVE_REQUERIDO, cambioClaveRequerido)
                .putLong(K_ULTIMA_VALIDACION, System.currentTimeMillis()).apply();
    }

    public boolean haySesionActiva() {
        if (!hayCredencialesGuardadas() || requiereCambioClave()) return false;
        long ultima = getUltimaValidacionServidor();
        long transcurrido = System.currentTimeMillis() - ultima;
        return ultima > 0 && transcurrido >= 0 && transcurrido <= VIGENCIA_OFFLINE_MS;
    }

    public boolean hayCredencialesGuardadas() {
        return !getUsuario().isEmpty() && !getToken().isEmpty();
    }

    public String getUsuario() { return prefs.getString(K_CORREO, ""); }
    public String getNombre() {
        String nombre = prefs.getString(K_NOMBRE, "");
        return nombre == null || nombre.isEmpty() ? getUsuario() : nombre;
    }
    public String getToken() { return prefs.getString(K_TOKEN, ""); }
    public String getPropietarioLocal() { return prefs.getString(K_PROPIETARIO_LOCAL, ""); }
    public boolean requiereCambioClave() {
        return prefs.getBoolean(K_CAMBIO_CLAVE_REQUERIDO, false);
    }
    public void cerrarSesion() {
        prefs.edit().remove(K_CORREO).remove(K_NOMBRE).remove(K_TOKEN)
                .remove(K_ULTIMA_VALIDACION).remove(K_CAMBIO_CLAVE_REQUERIDO).apply();
    }

    public void cerrarSesionCompletaLocal() {
        prefs.edit().clear().apply();
    }

    public void registrarValidacionServidor(long momento) {
        prefs.edit().putLong(K_ULTIMA_VALIDACION, momento).apply();
    }

    public long getUltimaValidacionServidor() {
        return prefs.getLong(K_ULTIMA_VALIDACION, -1L);
    }

    public void registrarSincronizacion(long momento) {
        prefs.edit().putLong(K_ULTIMA_SYNC, momento).apply();
    }
    public long getUltimaSincronizacion() { return prefs.getLong(K_ULTIMA_SYNC, -1L); }
}
