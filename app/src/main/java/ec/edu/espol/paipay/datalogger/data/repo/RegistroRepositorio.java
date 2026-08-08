package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;

import androidx.lifecycle.LiveData;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.SemaforoLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;

/** Fuente local de jornadas. Ninguna acción de formulario depende de internet. */
public class RegistroRepositorio {
    public interface AlGuardar {
        void listo(String uuid);
        default void error(String mensaje, boolean almacenamientoLleno) { }
    }
    public interface AlBuscar { void listo(JornadaConPeces jornada); }

    private final PaipayDatabase db;
    private final SesionManager sesion;
    private final AlmacenamientoRepositorio almacenamiento;

    public RegistroRepositorio(Context contexto) {
        db = PaipayDatabase.obtener(contexto);
        sesion = SesionManager.obtener(contexto);
        almacenamiento = new AlmacenamientoRepositorio(contexto);
    }

    public void guardar(JornadaLocal jornada, List<ObservacionPezLocal> peces,
                        boolean completar, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            if (almacenamiento.espacioCritico()) {
                fallar(callback, "Queda muy poco espacio para guardar con seguridad.", true);
                return;
            }
            try {
                JornadaConPeces anterior = db.jornadaDao().porUuid(jornada.uuid);
                if (anterior != null && anterior.jornada != null) {
                    jornada.creadaEn = anterior.jornada.creadaEn;
                    jornada.versionServidor = anterior.jornada.versionServidor;
                    jornada.autorCorreo = anterior.jornada.autorCorreo;
                } else {
                    jornada.creadaEn = System.currentTimeMillis();
                    jornada.autorCorreo = sesion.getUsuario();
                }
                jornada.modificadaEn = System.currentTimeMillis();
                if (completar) {
                    jornada.estadoLocal = jornada.versionServidor > 0
                            ? JornadaLocal.PENDIENTE_EDITAR : JornadaLocal.PENDIENTE_CREAR;
                } else {
                    jornada.estadoLocal = JornadaLocal.BORRADOR;
                }
                db.jornadaDao().guardar(jornada, peces);
                AppExecutors.enHiloPrincipal(() -> callback.listo(jornada.uuid));
            } catch (SQLiteFullException error) {
                fallar(callback, "El almacenamiento del teléfono está lleno.", true);
            } catch (SQLiteException error) {
                fallar(callback, "No se pudo guardar el borrador: " + error.getMessage(), false);
            }
        });
    }

    private void fallar(AlGuardar callback, String mensaje, boolean lleno) {
        AppExecutors.enHiloPrincipal(() -> callback.error(mensaje, lleno));
    }

    public void porUuid(String uuid, AlBuscar callback) {
        AppExecutors.io().execute(() -> {
            JornadaConPeces valor = db.jornadaDao().porUuid(uuid);
            AppExecutors.enHiloPrincipal(() -> callback.listo(valor));
        });
    }

    public void ultimoBorrador(AlBuscar callback) {
        AppExecutors.io().execute(() -> {
            JornadaConPeces valor = db.jornadaDao().ultimoBorrador(sesion.getUsuario());
            AppExecutors.enHiloPrincipal(() -> callback.listo(valor));
        });
    }

    public LiveData<List<JornadaConPeces>> jornadas() {
        return db.jornadaDao().observarHistorial(sesion.getUsuario());
    }

    public LiveData<List<PiscinaLocal>> piscinas() {
        return db.catalogoDao().observarPiscinasPeces();
    }

    public LiveData<List<SemaforoLocal>> semaforos() {
        return db.catalogoDao().observarSemaforos();
    }

    public void anular(String uuid, String motivo, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            try {
                JornadaConPeces item = db.jornadaDao().porUuid(uuid);
                if (item == null) {
                    fallar(callback, "No se encontró la jornada.", false);
                    return;
                }
                item.jornada.motivoCambio = motivo;
                item.jornada.modificadaEn = System.currentTimeMillis();
                item.jornada.estadoLocal = item.jornada.versionServidor > 0
                        ? JornadaLocal.PENDIENTE_ANULAR : JornadaLocal.ANULADO_LOCAL;
                db.jornadaDao().guardar(item.jornada, item.peces);
                AppExecutors.enHiloPrincipal(() -> callback.listo(uuid));
            } catch (SQLiteFullException error) {
                fallar(callback, "El almacenamiento del teléfono está lleno.", true);
            } catch (SQLiteException error) {
                fallar(callback, "No se pudo guardar la anulación: " + error.getMessage(), false);
            }
        });
    }
}
