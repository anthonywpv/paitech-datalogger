package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.data.local.entity.CamaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLombriculturaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroLombriculturaLocal;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;
import ec.edu.espol.paipay.datalogger.util.SeguridadUtil;

/** Operaciones offline de camas, ciclos y registros de lombricultura. */
public class LombriculturaRepositorio {
    public interface Callback<T> { void listo(T valor); }

    private final PaipayDatabase db;
    private final SesionManager sesion;

    public LombriculturaRepositorio(Context contexto) {
        db = PaipayDatabase.obtener(contexto);
        sesion = SesionManager.obtener(contexto);
    }

    public LiveData<List<CamaLocal>> camas() { return db.lombriculturaDao().observarCamas(); }
    public LiveData<List<RegistroLombriculturaLocal>> registros() {
        return db.lombriculturaDao().observarRegistros();
    }

    public void cicloActivo(String camaUuid, Callback<CicloLombriculturaLocal> callback) {
        AppExecutors.io().execute(() -> {
            CicloLombriculturaLocal ciclo = db.lombriculturaDao().cicloActivo(camaUuid);
            AppExecutors.enHiloPrincipal(() -> callback.listo(ciclo));
        });
    }

    public void ciclo(String uuid, Callback<CicloLombriculturaLocal> callback) {
        AppExecutors.io().execute(() -> {
            CicloLombriculturaLocal ciclo = db.lombriculturaDao().ciclo(uuid);
            AppExecutors.enHiloPrincipal(() -> callback.listo(ciclo));
        });
    }

    public void iniciarCiclo(CamaLocal cama, String iniciadoEn, int conteoInicial,
                             String observaciones, Callback<CicloLombriculturaLocal> callback) {
        AppExecutors.io().execute(() -> {
            CicloLombriculturaLocal ciclo = new CicloLombriculturaLocal();
            ciclo.uuid = SeguridadUtil.nuevoUuid();
            ciclo.camaUuid = cama.uuid;
            ciclo.camaCodigo = cama.codigo;
            ciclo.numero = db.lombriculturaDao().maxNumeroCiclo(cama.uuid) + 1;
            ciclo.iniciadoEn = iniciadoEn;
            ciclo.conteoInicial = conteoInicial;
            ciclo.observacionesApertura = observaciones;
            ciclo.autorCorreo = sesion.getUsuario();
            ciclo.estado = CicloLombriculturaLocal.ACTIVO;
            ciclo.estadoLocal = CicloLombriculturaLocal.PENDIENTE_CREAR;
            ciclo.creadaEn = System.currentTimeMillis();
            ciclo.modificadaEn = ciclo.creadaEn;
            db.lombriculturaDao().guardarCiclo(ciclo);
            cama.cicloActivoUuid = ciclo.uuid;
            cama.cicloActivoNumero = ciclo.numero;
            db.lombriculturaDao().guardarCama(cama);
            AppExecutors.enHiloPrincipal(() -> callback.listo(ciclo));
        });
    }

    public void cerrarCiclo(CamaLocal cama, CicloLombriculturaLocal ciclo,
                            String cerradoEn, int conteoFinal, String observaciones,
                            Callback<CicloLombriculturaLocal> callback) {
        AppExecutors.io().execute(() -> {
            ciclo.estado = CicloLombriculturaLocal.CERRADO;
            ciclo.cerradoEn = cerradoEn;
            ciclo.conteoFinal = conteoFinal;
            ciclo.observacionesCierre = observaciones;
            ciclo.estadoLocal = CicloLombriculturaLocal.PENDIENTE_CREAR.equals(ciclo.estadoLocal)
                    ? CicloLombriculturaLocal.PENDIENTE_CREAR_Y_CERRAR
                    : CicloLombriculturaLocal.PENDIENTE_CERRAR;
            ciclo.modificadaEn = System.currentTimeMillis();
            db.lombriculturaDao().guardarCiclo(ciclo);
            cama.cicloActivoUuid = null;
            cama.cicloActivoNumero = null;
            db.lombriculturaDao().guardarCama(cama);
            AppExecutors.enHiloPrincipal(() -> callback.listo(ciclo));
        });
    }

    public void guardarRegistro(CamaLocal cama, CicloLombriculturaLocal ciclo,
                                RegistroLombriculturaLocal anterior, String capturadaEn,
                                double phSuelo, int conteo, String observaciones,
                                Callback<RegistroLombriculturaLocal> callback) {
        AppExecutors.io().execute(() -> {
            RegistroLombriculturaLocal registro = anterior == null
                    ? new RegistroLombriculturaLocal() : anterior;
            if (anterior == null) {
                registro.uuid = SeguridadUtil.nuevoUuid();
                registro.creadaEn = System.currentTimeMillis();
                registro.versionServidor = 0;
            }
            registro.camaUuid = cama.uuid;
            registro.camaCodigo = cama.codigo;
            registro.cicloUuid = ciclo.uuid;
            registro.autorCorreo = sesion.getUsuario();
            registro.capturadaEn = capturadaEn;
            registro.phSuelo = phSuelo;
            registro.conteoLombrices = conteo;
            registro.observaciones = observaciones;
            registro.motivoCambio = anterior != null && anterior.versionServidor > 0
                    ? "Corrección realizada desde Android" : "";
            registro.estadoLocal = anterior != null && anterior.versionServidor > 0
                    ? RegistroLombriculturaLocal.PENDIENTE_EDITAR
                    : RegistroLombriculturaLocal.PENDIENTE_CREAR;
            registro.modificadaEn = System.currentTimeMillis();
            db.lombriculturaDao().guardarRegistro(registro);
            AppExecutors.enHiloPrincipal(() -> callback.listo(registro));
        });
    }

    public void anular(RegistroLombriculturaLocal registro, String motivo,
                       Callback<RegistroLombriculturaLocal> callback) {
        AppExecutors.io().execute(() -> {
            if (registro.versionServidor == 0) {
                registro.estadoLocal = RegistroLombriculturaLocal.ANULADO;
            } else {
                registro.estadoLocal = RegistroLombriculturaLocal.PENDIENTE_ANULAR;
            }
            registro.motivoCambio = motivo;
            registro.modificadaEn = System.currentTimeMillis();
            db.lombriculturaDao().guardarRegistro(registro);
            AppExecutors.enHiloPrincipal(() -> callback.listo(registro));
        });
    }
}
