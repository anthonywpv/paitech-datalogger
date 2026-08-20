package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;

import androidx.lifecycle.LiveData;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;

/** Fuente local de movimientos. Guardar, corregir y anular funciona sin internet. */
public class MovimientoRepositorio {
    public interface AlGuardar {
        void listo(String uuid);
        default void error(String mensaje, boolean almacenamientoLleno) { }
    }

    public interface AlBuscar { void listo(MovimientoLocal movimiento); }

    private final PaipayDatabase db;
    private final SesionManager sesion;
    private final AlmacenamientoRepositorio almacenamiento;

    public MovimientoRepositorio(Context contexto) {
        db = PaipayDatabase.obtener(contexto);
        sesion = SesionManager.obtener(contexto);
        almacenamiento = new AlmacenamientoRepositorio(contexto);
    }

    public LiveData<List<MovimientoLocal>> movimientos() {
        return db.movimientoDao().observar();
    }

    public LiveData<List<PiscinaLocal>> piscinas() {
        return db.catalogoDao().observarPiscinasPeces();
    }

    public void porUuid(String uuid, AlBuscar callback) {
        AppExecutors.io().execute(() -> {
            MovimientoLocal movimiento = db.movimientoDao().porUuid(uuid);
            AppExecutors.enHiloPrincipal(() -> callback.listo(movimiento));
        });
    }

    public void guardar(MovimientoLocal movimiento, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            if (almacenamiento.espacioCritico()) {
                fallar(callback, "Queda muy poco espacio para guardar con seguridad.", true);
                return;
            }
            try {
                MovimientoLocal anterior = db.movimientoDao().porUuid(movimiento.uuid);
                if (anterior == null) {
                    movimiento.autorCorreo = sesion.getUsuario();
                    movimiento.creadaEn = System.currentTimeMillis();
                } else {
                    movimiento.autorCorreo = anterior.autorCorreo;
                    movimiento.creadaEn = anterior.creadaEn;
                    movimiento.versionServidor = anterior.versionServidor;
                }
                movimiento.modificadaEn = System.currentTimeMillis();
                movimiento.estadoLocal = movimiento.versionServidor > 0
                        ? JornadaLocal.PENDIENTE_EDITAR : JornadaLocal.PENDIENTE_CREAR;
                db.movimientoDao().guardar(movimiento);
                AppExecutors.enHiloPrincipal(() -> callback.listo(movimiento.uuid));
            } catch (SQLiteFullException error) {
                fallar(callback, "El almacenamiento del teléfono está lleno.", true);
            } catch (SQLiteException error) {
                fallar(callback, "No se pudo guardar el movimiento: " + error.getMessage(), false);
            }
        });
    }

    public void anular(String uuid, String motivo, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            MovimientoLocal movimiento = db.movimientoDao().porUuid(uuid);
            if (movimiento == null) {
                fallar(callback, "No se encontró el movimiento.", false);
                return;
            }
            movimiento.motivoCambio = motivo;
            movimiento.modificadaEn = System.currentTimeMillis();
            movimiento.estadoLocal = movimiento.versionServidor > 0
                    ? JornadaLocal.PENDIENTE_ANULAR : JornadaLocal.ANULADO_LOCAL;
            try {
                db.movimientoDao().guardar(movimiento);
                AppExecutors.enHiloPrincipal(() -> callback.listo(uuid));
            } catch (SQLiteFullException error) {
                fallar(callback, "El almacenamiento del teléfono está lleno.", true);
            } catch (SQLiteException error) {
                fallar(callback, "No se pudo guardar la anulación: " + error.getMessage(), false);
            }
        });
    }

    private void fallar(AlGuardar callback, String mensaje, boolean lleno) {
        AppExecutors.enHiloPrincipal(() -> callback.error(mensaje, lleno));
    }
}
