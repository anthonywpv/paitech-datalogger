package ec.edu.espol.paipay.datalogger;

import android.app.Application;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;

/**
 * Clase Application de Paipay DataLogger.
 *
 * Proyecto de Prácticas Comunitarias — ESPOL
 * "Acuicultura y TICs: Expansión Tecnológica en el Cantón Santa Lucía y Daule"
 * Código PG15-PY26-04 · Recinto Paipayales
 *
 * Componente: Desarrollo de Aplicación Móvil con Sincronización Diferida (JAVA)
 */
public class PaipayApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Se abre la base local de una vez para que el primer formulario
        // no tenga que esperar la creación del archivo SQLite.
        PaipayDatabase.obtener(this);
    }
}
