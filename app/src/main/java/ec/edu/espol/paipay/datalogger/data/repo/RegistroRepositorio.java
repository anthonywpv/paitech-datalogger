package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.data.local.entity.EnsayoLaboratorio;
import ec.edu.espol.paipay.datalogger.data.local.entity.Piscina;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;
import ec.edu.espol.paipay.datalogger.util.SeguridadUtil;

/**
 * Punto único de escritura de registros.
 *
 * TODO registro se guarda SIEMPRE primero en la base local, marcado como
 * pendiente (sincronizado = false). La app nunca depende de la red para que
 * el productor pueda trabajar.
 *
 * OJO con registradoPor: aquí se deja VACÍO a propósito. La app no pide
 * credenciales para registrar en campo, así que al guardar todavía no se sabe
 * quién es; el correo se sella al SUBIR, en SincronizacionRepositorio, con el
 * de quien se identificó para esa subida. Que es además lo que interesa saber:
 * quién responde por ese dato en la base principal.
 */
public class RegistroRepositorio {

    public interface AlGuardar {
        void listo(long id);
    }

    private final PaipayDatabase db;
    private final SesionManager sesion;

    public RegistroRepositorio(Context contexto) {
        this.db = PaipayDatabase.obtener(contexto);
        this.sesion = SesionManager.obtener(contexto);
    }

    // ---------------------- ESCRITURA ----------------------

    public void guardarBiometria(RegistroBiometria r, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            r.uuid = SeguridadUtil.nuevoUuid();
            r.creadoEn = System.currentTimeMillis();
            r.sincronizado = false;
            long id = db.biometriaDao().insertar(r);
            AppExecutors.enHiloPrincipal(() -> callback.listo(id));
        });
    }

    public void guardarAgua(RegistroAgua r, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            r.uuid = SeguridadUtil.nuevoUuid();
            r.creadoEn = System.currentTimeMillis();
            r.sincronizado = false;
            long id = db.aguaDao().insertar(r);
            AppExecutors.enHiloPrincipal(() -> callback.listo(id));
        });
    }

    public void guardarLaboratorio(EnsayoLaboratorio e, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            e.uuid = SeguridadUtil.nuevoUuid();
            e.creadoEn = System.currentTimeMillis();
            e.sincronizado = false;
            long id = db.laboratorioDao().insertar(e);
            AppExecutors.enHiloPrincipal(() -> callback.listo(id));
        });
    }

    // ---------------------- EDICIÓN ----------------------

    /**
     * Corrige un registro ya guardado.
     *
     * Conserva el uuid y la fecha de creación original, y lo vuelve a marcar
     * como PENDIENTE aunque ya estuviera sincronizado. Eso no duplica nada: el
     * POST lleva Prefer: resolution=merge-duplicates, así que la próxima subida
     * hace UPSERT sobre ese mismo uuid y actualiza la fila que ya existe en
     * Neon. Es lo que permite arreglar un dato mal digitado de punta a punta.
     */
    public void actualizarBiometria(RegistroBiometria r, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            r.sincronizado = false;
            r.sincronizadoEn = null;
            db.biometriaDao().actualizar(r);
            AppExecutors.enHiloPrincipal(() -> callback.listo(r.id));
        });
    }

    public void actualizarAgua(RegistroAgua r, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            r.sincronizado = false;
            r.sincronizadoEn = null;
            db.aguaDao().actualizar(r);
            AppExecutors.enHiloPrincipal(() -> callback.listo(r.id));
        });
    }

    public void actualizarLaboratorio(EnsayoLaboratorio e, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            e.sincronizado = false;
            e.sincronizadoEn = null;
            db.laboratorioDao().actualizar(e);
            AppExecutors.enHiloPrincipal(() -> callback.listo(e.id));
        });
    }

    // ---------------------- LECTURA ----------------------

    public interface AlContar {
        void listo(int cuantos);
    }

    /** Peces ya medidos en el muestreo en curso (misma fecha y misma piscina). */
    public void contarEnMuestreo(String fechaIso, String piscina, AlContar callback) {
        AppExecutors.io().execute(() -> {
            int total = db.biometriaDao().contarEnMuestreo(fechaIso, piscina);
            AppExecutors.enHiloPrincipal(() -> callback.listo(total));
        });
    }

    /** Búsqueda puntual por uuid, para precargar el formulario de corrección. */
    public interface AlBuscar<T> {
        void listo(T registro);
    }

    public void biometriaPorUuid(String uuid, AlBuscar<RegistroBiometria> callback) {
        AppExecutors.io().execute(() -> {
            RegistroBiometria r = db.biometriaDao().porUuid(uuid);
            AppExecutors.enHiloPrincipal(() -> callback.listo(r));
        });
    }

    public void aguaPorUuid(String uuid, AlBuscar<RegistroAgua> callback) {
        AppExecutors.io().execute(() -> {
            RegistroAgua r = db.aguaDao().porUuid(uuid);
            AppExecutors.enHiloPrincipal(() -> callback.listo(r));
        });
    }

    public void laboratorioPorUuid(String uuid, AlBuscar<EnsayoLaboratorio> callback) {
        AppExecutors.io().execute(() -> {
            EnsayoLaboratorio e = db.laboratorioDao().porUuid(uuid);
            AppExecutors.enHiloPrincipal(() -> callback.listo(e));
        });
    }


    public LiveData<List<RegistroBiometria>> biometrias() {
        return db.biometriaDao().observarTodos();
    }

    public LiveData<List<RegistroAgua>> aguas() {
        return db.aguaDao().observarTodos();
    }

    public LiveData<List<EnsayoLaboratorio>> ensayos() {
        return db.laboratorioDao().observarTodos();
    }

    public LiveData<List<RegistroAgua>> ultimaMedicionPorPiscina() {
        return db.aguaDao().observarUltimaPorPiscina();
    }

    public LiveData<List<Piscina>> piscinas() {
        return db.piscinaDao().observarActivas();
    }
}
