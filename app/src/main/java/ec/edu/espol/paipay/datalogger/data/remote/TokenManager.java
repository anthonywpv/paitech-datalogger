package ec.edu.espol.paipay.datalogger.data.remote;

import android.content.Context;

import java.io.IOException;
import java.util.Map;

import retrofit2.Response;

/**
 * Custodio del JWT de Neon.
 *
 * Un JWT de Neon Auth vive ~15 minutos. Guardarlo sería inútil para una app
 * que puede pasar días sin conexión. Por eso:
 *
 *   · Lo permanente es la COOKIE de sesión (CookieJarPersistente).
 *   · El JWT se pide en el momento justo antes de sincronizar, se usa y se
 *     descarta cuando caduca.
 *
 * Resultado: el productor inicia sesión una sola vez y la app se las arregla
 * sola con los tokens.
 */
public class TokenManager {

    /** Margen de seguridad: se renueva 2 minutos antes de que caduque. */
    private static final long VIDA_UTIL_MS = 13 * 60 * 1000L;

    private static volatile TokenManager INSTANCIA;

    private final Context contexto;
    private String jwt;
    private long obtenidoEn;

    private TokenManager(Context contexto) {
        this.contexto = contexto.getApplicationContext();
    }

    public static TokenManager obtener(Context contexto) {
        if (INSTANCIA == null) {
            synchronized (TokenManager.class) {
                if (INSTANCIA == null) INSTANCIA = new TokenManager(contexto);
            }
        }
        return INSTANCIA;
    }

    /**
     * Devuelve un JWT válido, renovándolo contra Neon Auth si hace falta.
     * Devuelve null si la sesión ya no sirve (cookie caducada o revocada).
     *
     * Debe llamarse desde un hilo de trabajo, nunca desde el hilo principal.
     */
    public synchronized String jwtValido() {
        if (jwt != null && System.currentTimeMillis() - obtenidoEn < VIDA_UTIL_MS) {
            return jwt;
        }
        return renovar();
    }

    /**
     * Pide un JWT nuevo a Neon Auth.
     *
     * Better Auth no devuelve el JWT en el cuerpo sino en la cabecera HTTP
     * "set-auth-jwt" de la respuesta de get-session.
     */
    private String renovar() {
        try {
            Response<Map<String, Object>> respuesta =
                    NeonCliente.auth(contexto).obtenerSesion().execute();

            if (!respuesta.isSuccessful()) {
                invalidar();
                return null;
            }

            String nuevo = respuesta.headers().get("set-auth-jwt");
            if (nuevo == null || nuevo.isEmpty()) {
                nuevo = respuesta.headers().get("Set-Auth-Jwt");
            }
            if (nuevo == null || nuevo.isEmpty()) {
                invalidar();
                return null;
            }

            jwt = nuevo;
            obtenidoEn = System.currentTimeMillis();
            return jwt;

        } catch (IOException e) {
            // Sin red: no se puede renovar, pero la sesión sigue siendo válida.
            return null;
        }
    }

    public synchronized void invalidar() {
        jwt = null;
        obtenidoEn = 0L;
    }
}
