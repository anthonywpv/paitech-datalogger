package ec.edu.espol.paipay.datalogger;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

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

        // La app va SIEMPRE en modo claro, aunque el teléfono esté en oscuro.
        //
        // Dos razones. La primera es de campo: se usa al borde de la piscina,
        // bajo sol ecuatorial directo, y ahí un fondo claro se lee bastante
        // mejor que uno oscuro. La segunda es de marca: la paleta del
        // instructivo está pensada sobre blanco —el marrón #433116 es color de
        // TEXTO—, así que invertir el fondo dejaba texto oscuro sobre oscuro.
        //
        // Si algún día se quiere modo oscuro de verdad, hay que definir la
        // paleta completa con el equipo de FADCOM, no solo los fondos.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Se abre la base local de una vez para que el primer formulario
        // no tenga que esperar la creación del archivo SQLite.
        PaipayDatabase.obtener(this);
    }
}
