package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;

import androidx.lifecycle.LiveData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.domain.PrediccionCiclo;
import ec.edu.espol.paipay.datalogger.domain.PredictorCiclo;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;
import ec.edu.espol.paipay.datalogger.util.SeguridadUtil;

/** Apertura y cierre offline; la red solo interviene en la sincronización. */
public class CicloRepositorio {
    public interface AlGuardar {
        void listo(CicloLocal ciclo);
        default void error(String mensaje, boolean almacenamientoLleno) { }
    }
    public interface AlPredecir { void listo(PrediccionCiclo prediccion); }
    public interface AlBuscar { void listo(CicloLocal ciclo); }

    private final PaipayDatabase db;
    private final SesionManager sesion;
    private final AlmacenamientoRepositorio almacenamiento;

    public CicloRepositorio(Context contexto) {
        db = PaipayDatabase.obtener(contexto);
        sesion = SesionManager.obtener(contexto);
        almacenamiento = new AlmacenamientoRepositorio(contexto);
    }

    public LiveData<List<PiscinaLocal>> piscinas() {
        return db.catalogoDao().observarPiscinasPeces();
    }

    public void activo(String piscinaUuid, AlBuscar callback) {
        AppExecutors.io().execute(() -> {
            CicloLocal ciclo = db.cicloDao().activo(piscinaUuid);
            AppExecutors.enHiloPrincipal(() -> callback.listo(ciclo));
        });
    }

    public void predecir(String piscinaUuid, int poblacionInicial, AlPredecir callback) {
        AppExecutors.io().execute(() -> {
            List<CicloLocal> ciclos = db.cicloDao().cerradosParaPrediccion(piscinaUuid);
            Set<String> excluidos = new HashSet<>();
            for (CicloLocal ciclo : ciclos) {
                if (db.movimientoDao().contarTransferenciasOAjustes(ciclo.uuid) > 0) {
                    excluidos.add(ciclo.uuid);
                }
            }
            PrediccionCiclo resultado = PredictorCiclo.calcular(
                    poblacionInicial, ciclos, excluidos, System.currentTimeMillis());
            AppExecutors.enHiloPrincipal(() -> callback.listo(resultado));
        });
    }

    public void abrir(PiscinaLocal piscina, String iniciadoEn, int poblacionInicial,
                      Integer duracionMeses, String observaciones,
                      PrediccionCiclo prediccion, AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            if (almacenamiento.espacioCritico()) {
                fallar(callback, "Queda muy poco espacio para iniciar el ciclo.", true);
                return;
            }
            if (db.cicloDao().activo(piscina.uuid) != null || piscina.cicloActivoUuid != null) {
                fallar(callback, "La piscina ya tiene un ciclo activo.", false);
                return;
            }
            try {
                CicloLocal ciclo = new CicloLocal();
                ciclo.uuid = SeguridadUtil.nuevoUuid();
                ciclo.piscinaUuid = piscina.uuid;
                ciclo.piscinaCodigo = piscina.codigo;
                ciclo.especieNombre = piscina.especieNombre;
                ciclo.numero = db.cicloDao().maxNumero(piscina.uuid) + 1;
                ciclo.estado = CicloLocal.ACTIVO;
                ciclo.iniciadoEn = iniciadoEn;
                ciclo.poblacionInicial = poblacionInicial;
                ciclo.duracionEstimadaMeses = duracionMeses;
                ciclo.observacionesApertura = observaciones;
                ciclo.autorCorreo = sesion.getUsuario();
                ciclo.estadoLocal = CicloLocal.PENDIENTE_CREAR;
                ciclo.creadaEn = System.currentTimeMillis();
                ciclo.modificadaEn = ciclo.creadaEn;
                aplicarPrediccion(ciclo, prediccion);
                piscina.cicloActivoUuid = ciclo.uuid;
                piscina.cicloActivoNumero = ciclo.numero;
                db.runInTransaction(() -> {
                    db.cicloDao().guardar(ciclo);
                    db.catalogoDao().guardarPiscina(piscina);
                });
                AppExecutors.enHiloPrincipal(() -> callback.listo(ciclo));
            } catch (SQLiteFullException error) {
                fallar(callback, "El almacenamiento del teléfono está lleno.", true);
            } catch (SQLiteException error) {
                fallar(callback, "No se pudo guardar el ciclo: " + error.getMessage(), false);
            }
        });
    }

    public void cerrar(PiscinaLocal piscina, CicloLocal ciclo, String cerradoEn,
                       String destino, int poblacionFinal, Double pesoKg,
                       String observaciones, String piscinaDestinoUuid,
                       AlGuardar callback) {
        AppExecutors.io().execute(() -> {
            try {
                CicloLocal actual = db.cicloDao().porUuid(ciclo.uuid);
                if (actual == null || !CicloLocal.ACTIVO.equals(actual.estado)) {
                    fallar(callback, "El ciclo ya no está activo.", false);
                    return;
                }
                actual.estado = CicloLocal.CERRADO;
                actual.cerradoEn = cerradoEn;
                actual.destinoCierre = destino;
                actual.poblacionFinal = poblacionFinal;
                actual.pesoTotalCosechadoKg = pesoKg;
                actual.observacionesCierre = observaciones;
                actual.piscinaDestinoCierreUuid = piscinaDestinoUuid;
                actual.autorCorreo = sesion.getUsuario();
                actual.modificadaEn = System.currentTimeMillis();
                actual.estadoLocal = CicloLocal.PENDIENTE_CREAR.equals(actual.estadoLocal)
                        ? CicloLocal.PENDIENTE_CREAR_Y_CERRAR
                        : CicloLocal.PENDIENTE_CERRAR;
                piscina.cicloActivoUuid = null;
                piscina.cicloActivoNumero = null;
                db.runInTransaction(() -> {
                    db.cicloDao().guardar(actual);
                    db.catalogoDao().guardarPiscina(piscina);
                });
                AppExecutors.enHiloPrincipal(() -> callback.listo(actual));
            } catch (SQLiteFullException error) {
                fallar(callback, "El almacenamiento del teléfono está lleno.", true);
            } catch (SQLiteException error) {
                fallar(callback, "No se pudo cerrar el ciclo: " + error.getMessage(), false);
            }
        });
    }

    private void aplicarPrediccion(CicloLocal ciclo, PrediccionCiclo p) {
        ciclo.prediccionPoblacionFinal = p.estimacion;
        ciclo.prediccionMin = p.minimo;
        ciclo.prediccionMax = p.maximo;
        ciclo.prediccionTasa = p.tasa;
        ciclo.prediccionCiclosUsados = p.ciclosUsados;
        ciclo.prediccionConfianza = p.confianza;
        ciclo.prediccionMetodoVersion = PredictorCiclo.METODO;
        ciclo.prediccionCalculadaEn = p.calculadaEn;
        ciclo.prediccionDatosHasta = p.datosHasta;
        ciclo.prediccionOrigen = "CACHE_ANDROID";
    }

    private void fallar(AlGuardar callback, String mensaje, boolean lleno) {
        AppExecutors.enHiloPrincipal(() -> callback.error(mensaje, lleno));
    }
}
