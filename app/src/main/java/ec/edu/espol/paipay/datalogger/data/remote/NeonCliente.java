package ec.edu.espol.paipay.datalogger.data.remote;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import ec.edu.espol.paipay.datalogger.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Fábrica de los dos clientes HTTP del proyecto:
 *
 *   · auth(...) → Neon Auth   (login y renovación de JWT)
 *   · datos(...) → Neon Data API (PostgREST sobre la base Postgres)
 *
 * Ambos comparten el mismo CookieJar persistente, que es lo que mantiene viva
 * la sesión del productor entre reinicios del teléfono.
 */
public final class NeonCliente {

    private static volatile CookieJarPersistente COOKIES;
    private static volatile NeonAuthService AUTH;
    private static volatile NeonApiService DATOS;

    private NeonCliente() { }

    // ------------------------------------------------------------------

    public static CookieJarPersistente cookies(Context contexto) {
        if (COOKIES == null) {
            synchronized (NeonCliente.class) {
                if (COOKIES == null) COOKIES = new CookieJarPersistente(contexto);
            }
        }
        return COOKIES;
    }

    public static NeonAuthService auth(Context contexto) {
        if (AUTH == null) {
            synchronized (NeonCliente.class) {
                if (AUTH == null) AUTH = construirAuth(contexto);
            }
        }
        return AUTH;
    }

    public static NeonApiService datos(Context contexto) {
        if (DATOS == null) {
            synchronized (NeonCliente.class) {
                if (DATOS == null) DATOS = construirDatos(contexto);
            }
        }
        return DATOS;
    }

    // ------------------------------------------------------------------

    private static HttpLoggingInterceptor registro() {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BASIC
                : HttpLoggingInterceptor.Level.NONE);
        return log;
    }

    private static OkHttpClient.Builder base(Context contexto) {
        return new OkHttpClient.Builder()
                // Tiempos generosos: la señal en Paipayales es débil e intermitente.
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .cookieJar(cookies(contexto));
    }

    private static NeonAuthService construirAuth(Context contexto) {
        OkHttpClient http = base(contexto)
                .addInterceptor(cadena -> {
                    Request original = cadena.request();
                    return cadena.proceed(original.newBuilder()
                            // Better Auth exige un Origin declarado como confiable.
                            .header("Origin", BuildConfig.NEON_ORIGIN)
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .build());
                })
                .addInterceptor(registro())
                .build();

        return new Retrofit.Builder()
                .baseUrl(conBarra(BuildConfig.NEON_AUTH_URL))
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NeonAuthService.class);
    }

    private static NeonApiService construirDatos(Context contexto) {
        final Context app = contexto.getApplicationContext();

        OkHttpClient http = base(contexto)
                .addInterceptor(cadena -> {
                    // Se adjunta un JWT fresco a cada petición a la Data API.
                    String jwt = TokenManager.obtener(app).jwtValido();
                    if (jwt == null) {
                        throw new SesionExpiradaException();
                    }
                    Request original = cadena.request();
                    return cadena.proceed(original.newBuilder()
                            .header("Authorization", "Bearer " + jwt)
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .build());
                })
                .addInterceptor(registro())
                .build();

        return new Retrofit.Builder()
                .baseUrl(conBarra(BuildConfig.NEON_DATA_API_URL))
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NeonApiService.class);
    }

    private static String conBarra(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    /** Se llama al cerrar sesión: borra cookies y obliga a reconstruir clientes. */
    public static synchronized void reiniciar(Context contexto) {
        cookies(contexto).limpiar();
        TokenManager.obtener(contexto).invalidar();
        AUTH = null;
        DATOS = null;
    }
}
