package ec.edu.espol.paipay.datalogger.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import ec.edu.espol.paipay.datalogger.BuildConfig;

/**
 * Detección de conectividad.
 *
 * En Paipayales la señal es intermitente, así que no basta con saber si hay
 * una red asociada: se verifica además que esa red tenga Internet validado.
 */
public final class RedUtil {

    private RedUtil() { }

    public static boolean hayInternet(Context contexto) {
        ConnectivityManager cm = (ConnectivityManager)
                contexto.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network red = cm.getActiveNetwork();
        if (red == null) return false;
        NetworkCapabilities cap = cm.getNetworkCapabilities(red);
        if (cap == null) return false;
        boolean transporte = cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
        boolean declaraInternet = cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        boolean validada = cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        boolean backendLocalDebug = BuildConfig.DEBUG
                && BuildConfig.API_BASE_URL.startsWith("http://10.0.2.2");
        return esRedUtilizable(transporte, declaraInternet, validada, backendLocalDebug);
    }

    /**
     * Producción exige la validación de Android. El único caso excepcional es
     * el backend local del AVD en debug: puede alcanzar 10.0.2.2 aunque el AVD
     * no tenga salida al comprobador público que marca la red como validada.
     */
    static boolean esRedUtilizable(
            boolean transporte,
            boolean declaraInternet,
            boolean validada,
            boolean backendLocalDebug
    ) {
        return transporte && declaraInternet && (validada || backendLocalDebug);
    }

    /** true si la conexión es por datos móviles (útil para advertir sobre consumo). */
    public static boolean esDatosMoviles(Context contexto) {
        ConnectivityManager cm = (ConnectivityManager)
                contexto.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network red = cm.getActiveNetwork();
        if (red == null) return false;
        NetworkCapabilities cap = cm.getNetworkCapabilities(red);
        return cap != null && cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
    }
}
