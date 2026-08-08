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
        prefs.edit().putString(K_CORREO, correo).putString(K_NOMBRE, nombre)
                .putString(K_TOKEN, token).apply();
    }

    public boolean haySesionActiva() {
        return !getUsuario().isEmpty() && !getToken().isEmpty();
    }

    public String getUsuario() { return prefs.getString(K_CORREO, ""); }
    public String getNombre() {
        String nombre = prefs.getString(K_NOMBRE, "");
        return nombre == null || nombre.isEmpty() ? getUsuario() : nombre;
    }
    public String getToken() { return prefs.getString(K_TOKEN, ""); }
    public void cerrarSesion() {
        prefs.edit().remove(K_CORREO).remove(K_NOMBRE).remove(K_TOKEN).apply();
    }

    public void registrarSincronizacion(long momento) {
        prefs.edit().putLong(K_ULTIMA_SYNC, momento).apply();
    }
    public long getUltimaSincronizacion() { return prefs.getLong(K_ULTIMA_SYNC, -1L); }
}
