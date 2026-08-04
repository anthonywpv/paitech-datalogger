package ec.edu.espol.paipay.datalogger.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Sincronización automática en segundo plano.
 *
 * Complementa (no reemplaza) al botón [Sincronizar]: si el productor deja
 * datos pendientes y el teléfono recupera conexión más tarde, el sistema
 * operativo despierta este Worker y los sube solo.
 *
 * El botón manual sigue siendo el camino principal, porque el proyecto exige
 * que el usuario vea y confirme qué se va a subir.
 */
public class SincronizacionWorker extends Worker {

    public static final String NOMBRE_TRABAJO = "paipay_sync_automatica";

    public SincronizacionWorker(@NonNull Context contexto, @NonNull WorkerParameters params) {
        super(contexto, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        final CountDownLatch cerrojo = new CountDownLatch(1);
        final ResultadoSincronizacion[] salida = new ResultadoSincronizacion[1];

        new SincronizacionRepositorio(getApplicationContext())
                .sincronizar(new SincronizacionRepositorio.CallbackSincronizacion() {
                    @Override
                    public void progreso(String mensaje) { /* silencioso */ }

                    @Override
                    public void terminado(ResultadoSincronizacion resultado) {
                        salida[0] = resultado;
                        cerrojo.countDown();
                    }
                });

        try {
            cerrojo.await(3, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }

        if (salida[0] == null) return Result.retry();

        switch (salida[0].estado) {
            case EXITO:
            case SIN_PENDIENTES:
                return Result.success();
            case SESION_EXPIRADA:
                // Reintentar no sirve: hace falta que el productor vuelva a
                // iniciar sesión desde la app.
                return Result.failure();
            case SIN_INTERNET:
            case PARCIAL:
            case ERROR:
            default:
                return Result.retry();
        }
    }

    /** Encola un intento de subida para cuando haya red disponible. */
    public static void programar(Context contexto) {
        Constraints condiciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest trabajo = new OneTimeWorkRequest.Builder(SincronizacionWorker.class)
                .setConstraints(condiciones)
                .setBackoffCriteria(androidx.work.BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(contexto.getApplicationContext())
                .enqueueUniqueWork(NOMBRE_TRABAJO, ExistingWorkPolicy.KEEP, trabajo);
    }
}
