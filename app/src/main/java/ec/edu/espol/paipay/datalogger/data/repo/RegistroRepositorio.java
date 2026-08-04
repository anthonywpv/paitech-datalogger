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
            r.registradoPor = sesion.getUsuario();
            r.sincronizado = false;
            long id = db.biometriaDao().insertar(r);
            AppExecutors.enHiloPrincipal(() -> callback.listo(id));
        });
    }

    public void guardarAgua(RegistroAgua r, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            r.uuid = SeguridadUtil.nuevoUuid();
            r.creadoEn = System.currentTimeMillis();
            r.registradoPor = sesion.getUsuario();
            r.sincronizado = false;
            long id = db.aguaDao().insertar(r);
            AppExecutors.enHiloPrincipal(() -> callback.listo(id));
        });
    }

    public void guardarLaboratorio(EnsayoLaboratorio e, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            e.uuid = SeguridadUtil.nuevoUuid();
            e.creadoEn = System.currentTimeMillis();
            e.registradoPor = sesion.getUsuario();
            e.sincronizado = false;
            long id = db.laboratorioDao().insertar(e);
            AppExecutors.enHiloPrincipal(() -> callback.listo(id));
        });
    }

    // ---------------------- LECTURA ----------------------

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
