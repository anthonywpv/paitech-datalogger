package ec.edu.espol.paipay.datalogger.data.remote;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import ec.edu.espol.paipay.datalogger.BuildConfig;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Único cliente de red: Android habla con Django, nunca directamente con Neon. */
public final class DjangoCliente {
    private static volatile DjangoApiService API;

    private DjangoCliente() { }

    public static DjangoApiService api(Context contexto) {
        if (API == null) {
            synchronized (DjangoCliente.class) {
                if (API == null) API = construir(contexto.getApplicationContext());
            }
        }
        return API;
    }

    private static DjangoApiService construir(Context contexto) {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC
                : HttpLoggingInterceptor.Level.NONE);
        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(cadena -> {
                    Request original = cadena.request();
                    Request.Builder nueva = original.newBuilder()
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json");
                    String token = SesionManager.obtener(contexto).getToken();
                    if (token != null && !token.isEmpty()) {
                        nueva.header("Authorization", "Token " + token);
                    }
                    return cadena.proceed(nueva.build());
                })
                .addInterceptor(log)
                .build();
        String base = BuildConfig.API_BASE_URL.endsWith("/")
                ? BuildConfig.API_BASE_URL : BuildConfig.API_BASE_URL + "/";
        return new Retrofit.Builder()
                .baseUrl(base)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DjangoApiService.class);
    }

    public static synchronized void reiniciar() {
        API = null;
    }
}
