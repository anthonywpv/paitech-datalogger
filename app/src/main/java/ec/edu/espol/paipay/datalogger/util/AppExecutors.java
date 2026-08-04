package ec.edu.espol.paipay.datalogger.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pool de hilos de la aplicación.
 *
 * Regla de oro en Android: ninguna operación de base de datos ni de red
 * puede ejecutarse en el hilo principal (bloquearía la interfaz).
 */
public final class AppExecutors {

    private static final ExecutorService IO = Executors.newFixedThreadPool(4);
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private AppExecutors() { }

    /** Hilos de trabajo: Room, Retrofit, archivos. */
    public static ExecutorService io() {
        return IO;
    }

    /** Hilo de interfaz: actualizar vistas, mostrar diálogos. */
    public static void enHiloPrincipal(Runnable tarea) {
        PRINCIPAL.post(tarea);
    }
}
