package ec.edu.espol.paipay.datalogger.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Almacén de cookies que sobrevive al cierre de la app.
 *
 * ES LA PIEZA QUE HACE POSIBLE "INICIAR SESIÓN UNA SOLA VEZ":
 * Neon Auth (Better Auth) entrega una cookie de sesión de larga duración al
 * hacer sign-in. Guardándola cifrada en el teléfono, la app puede pedir un
 * JWT nuevo cada vez que necesite sincronizar, sin volver a pedirle la
 * contraseña al productor.
 *
 * Las cookies se guardan en EncryptedSharedPreferences (AES-256).
 */
public class CookieJarPersistente implements CookieJar {

    private static final String ARCHIVO = "paipay_cookies";
    private static final String CLAVE = "cookies_sesion";

    private final SharedPreferences prefs;
    private final Set<String> memoria = new HashSet<>();

    public CookieJarPersistente(Context contexto) {
        SharedPreferences p;
        try {
            MasterKey llave = new MasterKey.Builder(contexto.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            p = EncryptedSharedPreferences.create(
                    contexto.getApplicationContext(), ARCHIVO, llave,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            p = contexto.getApplicationContext()
                    .getSharedPreferences(ARCHIVO + "_plano", Context.MODE_PRIVATE);
        }
        this.prefs = p;
        Set<String> guardadas = prefs.getStringSet(CLAVE, null);
        if (guardadas != null) memoria.addAll(guardadas);
    }

    @Override
    public synchronized void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        if (cookies.isEmpty()) return;
        for (Cookie c : cookies) {
            // Se reemplaza cualquier versión anterior de la misma cookie
            memoria.removeIf(s -> s.startsWith(c.name() + "="));
            memoria.add(c.toString());
        }
        persistir();
    }

    @NonNull
    @Override
    public synchronized List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        List<Cookie> vigentes = new ArrayList<>();
        List<String> expiradas = new ArrayList<>();

        for (String bruta : memoria) {
            Cookie c = Cookie.parse(url, bruta);
            if (c == null) continue;
            if (c.expiresAt() < System.currentTimeMillis()) {
                expiradas.add(bruta);
            } else if (c.matches(url)) {
                vigentes.add(c);
            }
        }

        if (!expiradas.isEmpty()) {
            memoria.removeAll(expiradas);
            persistir();
        }
        return vigentes;
    }

    /** Borra la sesión guardada (al cerrar sesión). */
    public synchronized void limpiar() {
        memoria.clear();
        prefs.edit().remove(CLAVE).apply();
    }

    public synchronized boolean haySesionGuardada() {
        return !memoria.isEmpty();
    }

    private void persistir() {
        prefs.edit().putStringSet(CLAVE, new HashSet<>(memoria)).apply();
    }
}
