package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;
import android.database.sqlite.SQLiteException;

import com.google.gson.Gson;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.data.local.entity.ConflictoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;
import ec.edu.espol.paipay.datalogger.data.remote.DjangoApiService;
import ec.edu.espol.paipay.datalogger.data.remote.DjangoCliente;
import ec.edu.espol.paipay.datalogger.data.remote.dto.JornadaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CicloApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.MovimientoApiDto;
import ec.edu.espol.paipay.datalogger.domain.ComparadorConflictos;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;
import ec.edu.espol.paipay.datalogger.util.RedUtil;
import retrofit2.Response;

import java.util.List;

/** Conserva, compara y resuelve conflictos sin sobrescrituras automáticas. */
public class ConflictoRepositorio {
    public interface AlCargar {
        void listo(DetalleConflicto detalle);
        void error(String mensaje);
    }

    public interface AlResolver {
        void listo();
        void error(String mensaje);
    }

    private final Context contexto;
    private final PaipayDatabase db;
    private final SesionManager sesion;
    private final Gson gson = new Gson();

    public ConflictoRepositorio(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        db = PaipayDatabase.obtener(contexto);
        sesion = SesionManager.obtener(contexto);
    }

    public void cargar(String tipo, String uuid, AlCargar callback) {
        AppExecutors.io().execute(() -> {
            try {
                ConflictoLocal conflicto = obtenerInstantanea(tipo, uuid);
                DetalleConflicto detalle = ConflictoLocal.JORNADA.equals(tipo)
                        ? detalleJornada(conflicto)
                        : ConflictoLocal.CICLO.equals(tipo)
                        ? detalleCiclo(conflicto) : detalleMovimiento(conflicto);
                AppExecutors.enHiloPrincipal(() -> callback.listo(detalle));
            } catch (Exception error) {
                String mensaje = mensaje(error);
                AppExecutors.enHiloPrincipal(() -> callback.error(mensaje));
            }
        });
    }

    public void descartarCambioLocal(String tipo, String uuid, AlResolver callback) {
        AppExecutors.io().execute(() -> {
            try {
                ConflictoLocal conflicto = conflicto(tipo, uuid);
                if (conflicto.remotoJson == null) {
                    throw new IllegalStateException("Primero abre la comparación con Internet.");
                }
                if (ConflictoLocal.JORNADA.equals(tipo)) {
                    JornadaConPeces local = db.jornadaDao().porUuid(uuid);
                    validarAutor(local == null || local.jornada == null
                            ? null : local.jornada.autorCorreo);
                } else if (ConflictoLocal.CICLO.equals(tipo)) {
                    CicloLocal local = db.cicloDao().porUuid(uuid);
                    validarAutor(local == null ? null : local.autorCorreo);
                } else {
                    MovimientoLocal local = db.movimientoDao().porUuid(uuid);
                    validarAutor(local == null ? null : local.autorCorreo);
                }
                db.runInTransaction(() -> {
                    if (ConflictoLocal.JORNADA.equals(tipo)) {
                        JornadaApiDto dto = gson.fromJson(conflicto.remotoJson, JornadaApiDto.class);
                        PiscinaLocal piscina = db.catalogoDao().piscina(dto.piscina);
                        JornadaConPeces remota = MapeadorApi.aLocal(dto, piscina, sesion.getUsuario());
                        db.jornadaDao().guardar(remota.jornada, remota.peces);
                    } else if (ConflictoLocal.CICLO.equals(tipo)) {
                        CicloLocal local = db.cicloDao().porUuid(uuid);
                        CicloApiDto dto = gson.fromJson(
                                conflicto.remotoJson, CicloApiDto.class);
                        CicloLocal remota = MapeadorApi.aLocal(dto, sesion.getUsuario());
                        PiscinaLocal piscina = db.catalogoDao().piscina(local.piscinaUuid);
                        // Si dos teléfonos abrieron ciclos distintos offline, las
                        // operaciones aún pendientes del ciclo descartado se enlazan
                        // conscientemente al ciclo activo que se adopta del servidor.
                        db.jornadaDao().reasignarCiclo(local.uuid, remota.uuid);
                        db.movimientoDao().reasignarCicloOrigen(local.uuid, remota.uuid);
                        db.movimientoDao().reasignarCicloDestino(local.uuid, remota.uuid);
                        db.cicloDao().borrar(local.uuid);
                        db.cicloDao().guardar(remota);
                        if (piscina != null) {
                            if (CicloLocal.ACTIVO.equals(remota.estado)) {
                                piscina.cicloActivoUuid = remota.uuid;
                                piscina.cicloActivoNumero = remota.numero;
                            } else if (local.uuid.equals(piscina.cicloActivoUuid)) {
                                piscina.cicloActivoUuid = null;
                                piscina.cicloActivoNumero = null;
                            }
                            db.catalogoDao().guardarPiscina(piscina);
                        }
                    } else {
                        MovimientoApiDto dto = gson.fromJson(
                                conflicto.remotoJson, MovimientoApiDto.class);
                        db.movimientoDao().guardar(MapeadorApi.aLocal(dto, sesion.getUsuario()));
                    }
                    db.conflictoDao().borrar(conflicto.clave);
                });
                resolverListo(callback);
            } catch (Exception error) {
                resolverError(callback, error);
            }
        });
    }

    public void reaplicarCambioLocal(String tipo, String uuid, AlResolver callback) {
        AppExecutors.io().execute(() -> {
            try {
                ConflictoLocal conflicto = conflicto(tipo, uuid);
                if (conflicto.remotoJson == null) {
                    throw new IllegalStateException("Primero abre la comparación con Internet.");
                }
                if (ConflictoLocal.JORNADA.equals(tipo)) {
                    JornadaApiDto remota = gson.fromJson(conflicto.remotoJson, JornadaApiDto.class);
                    if ("ANULADA".equals(remota.estado)) {
                        throw new IllegalStateException(
                                "La jornada ya fue anulada y no puede sobrescribirse.");
                    }
                    JornadaConPeces local = db.jornadaDao().porUuid(uuid);
                    validarAutor(local == null ? null : local.jornada.autorCorreo);
                    db.runInTransaction(() -> {
                        local.jornada.versionServidor = remota.version;
                        local.jornada.estadoLocal = operacionParaReaplicar(
                                conflicto.operacionLocal);
                        local.jornada.errorSincronizacion = null;
                        local.jornada.modificadaEn = System.currentTimeMillis();
                        db.jornadaDao().guardar(local.jornada, local.peces);
                        db.conflictoDao().borrar(conflicto.clave);
                    });
                } else if (ConflictoLocal.CICLO.equals(tipo)) {
                    CicloApiDto remota = gson.fromJson(
                            conflicto.remotoJson, CicloApiDto.class);
                    CicloLocal local = db.cicloDao().porUuid(uuid);
                    validarAutor(local == null ? null : local.autorCorreo);
                    boolean cierreLocal = CicloLocal.PENDIENTE_CERRAR.equals(
                            conflicto.operacionLocal)
                            || CicloLocal.PENDIENTE_CREAR_Y_CERRAR.equals(
                            conflicto.operacionLocal);
                    if (!cierreLocal || !local.uuid.equals(remota.id)
                            || !CicloLocal.ACTIVO.equals(remota.estado)) {
                        throw new IllegalStateException(
                                "Este conflicto de apertura no puede sobrescribir otro ciclo "
                                        + "activo. Descarta el cambio local y revisa el ciclo del servidor.");
                    }
                    db.runInTransaction(() -> {
                        local.versionServidor = remota.version == null ? 0 : remota.version;
                        local.estadoLocal = CicloLocal.PENDIENTE_CERRAR;
                        local.errorSincronizacion = null;
                        local.modificadaEn = System.currentTimeMillis();
                        db.cicloDao().guardar(local);
                        db.conflictoDao().borrar(conflicto.clave);
                    });
                } else {
                    MovimientoApiDto remoto = gson.fromJson(
                            conflicto.remotoJson, MovimientoApiDto.class);
                    if ("ANULADO".equals(remoto.estado)) {
                        throw new IllegalStateException(
                                "El movimiento ya fue anulado y no puede sobrescribirse.");
                    }
                    MovimientoLocal local = db.movimientoDao().porUuid(uuid);
                    validarAutor(local == null ? null : local.autorCorreo);
                    db.runInTransaction(() -> {
                        local.versionServidor = remoto.version;
                        local.estadoLocal = operacionParaReaplicar(conflicto.operacionLocal);
                        local.errorSincronizacion = null;
                        local.modificadaEn = System.currentTimeMillis();
                        db.movimientoDao().guardar(local);
                        db.conflictoDao().borrar(conflicto.clave);
                    });
                }
                resolverListo(callback);
            } catch (Exception error) {
                resolverError(callback, error);
            }
        });
    }

    private ConflictoLocal obtenerInstantanea(String tipo, String uuid) throws Exception {
        ConflictoLocal conflicto = db.conflictoDao().porClave(ConflictoLocal.clave(tipo, uuid));
        if (conflicto == null) {
            conflicto = new ConflictoLocal();
            conflicto.clave = ConflictoLocal.clave(tipo, uuid);
            conflicto.tipo = tipo;
            conflicto.entidadUuid = uuid;
            conflicto.operacionLocal = JornadaLocal.PENDIENTE_EDITAR;
            conflicto.detectadoEn = System.currentTimeMillis();
        }
        if (conflicto.remotoJson != null) return conflicto;
        if (!RedUtil.hayInternet(contexto)) {
            throw new IllegalStateException(
                    "Necesitas Internet una vez para descargar la versión del servidor. "
                            + "Tu cambio sigue guardado en el teléfono.");
        }
        DjangoApiService api = DjangoCliente.api(contexto);
        if (ConflictoLocal.JORNADA.equals(tipo)) {
            Response<JornadaApiDto> respuesta = api.jornada(uuid).execute();
            if (!respuesta.isSuccessful() || respuesta.body() == null) {
                throw new IllegalStateException("No se pudo descargar la jornada actual (HTTP "
                        + respuesta.code() + ").");
            }
            conflicto.versionRemota = respuesta.body().version;
            conflicto.remotoJson = gson.toJson(respuesta.body());
        } else if (ConflictoLocal.CICLO.equals(tipo)) {
            Response<CicloApiDto> respuesta = api.ciclo(uuid).execute();
            CicloApiDto remota = respuesta.isSuccessful() ? respuesta.body() : null;
            if (remota == null) {
                CicloLocal local = db.cicloDao().porUuid(uuid);
                Response<List<CicloApiDto>> lista = api.ciclos().execute();
                if (lista.isSuccessful() && lista.body() != null && local != null) {
                    for (CicloApiDto candidata : lista.body()) {
                        if (local.piscinaUuid.equals(candidata.piscina)
                                && CicloLocal.ACTIVO.equals(candidata.estado)) {
                            remota = candidata;
                            break;
                        }
                    }
                }
            }
            if (remota == null) {
                throw new IllegalStateException(
                        "No se encontró el ciclo que produjo el conflicto en el servidor.");
            }
            conflicto.versionRemota = remota.version == null ? 0 : remota.version;
            conflicto.remotoJson = gson.toJson(remota);
        } else {
            Response<MovimientoApiDto> respuesta = api.movimiento(uuid).execute();
            if (!respuesta.isSuccessful() || respuesta.body() == null) {
                throw new IllegalStateException("No se pudo descargar el movimiento actual (HTTP "
                        + respuesta.code() + ").");
            }
            conflicto.versionRemota = respuesta.body().version;
            conflicto.remotoJson = gson.toJson(respuesta.body());
        }
        db.conflictoDao().guardar(conflicto);
        return conflicto;
    }

    private DetalleConflicto detalleJornada(ConflictoLocal conflicto) {
        JornadaConPeces local = db.jornadaDao().porUuid(conflicto.entidadUuid);
        if (local == null || local.jornada == null) {
            throw new IllegalStateException("No se encontró el cambio local de la jornada.");
        }
        validarAutor(local.jornada.autorCorreo);
        JornadaApiDto remota = gson.fromJson(conflicto.remotoJson, JornadaApiDto.class);
        boolean reaplicable = !"ANULADA".equals(remota.estado);
        return new DetalleConflicto(conflicto.tipo, conflicto.entidadUuid,
                ComparadorConflictos.jornada(local, remota), remota.version,
                reaplicable, remota.estado);
    }

    private DetalleConflicto detalleMovimiento(ConflictoLocal conflicto) {
        MovimientoLocal local = db.movimientoDao().porUuid(conflicto.entidadUuid);
        if (local == null) {
            throw new IllegalStateException("No se encontró el cambio local del movimiento.");
        }
        validarAutor(local.autorCorreo);
        MovimientoApiDto remoto = gson.fromJson(
                conflicto.remotoJson, MovimientoApiDto.class);
        boolean reaplicable = !"ANULADO".equals(remoto.estado);
        return new DetalleConflicto(conflicto.tipo, conflicto.entidadUuid,
                ComparadorConflictos.movimiento(local, remoto), remoto.version,
                reaplicable, remoto.estado);
    }

    private DetalleConflicto detalleCiclo(ConflictoLocal conflicto) {
        CicloLocal local = db.cicloDao().porUuid(conflicto.entidadUuid);
        if (local == null) {
            throw new IllegalStateException("No se encontró el cambio local del ciclo.");
        }
        validarAutor(local.autorCorreo);
        CicloApiDto remoto = gson.fromJson(conflicto.remotoJson, CicloApiDto.class);
        boolean cierreLocal = CicloLocal.PENDIENTE_CERRAR.equals(conflicto.operacionLocal)
                || CicloLocal.PENDIENTE_CREAR_Y_CERRAR.equals(conflicto.operacionLocal);
        boolean reaplicable = cierreLocal && local.uuid.equals(remoto.id)
                && CicloLocal.ACTIVO.equals(remoto.estado);
        return new DetalleConflicto(conflicto.tipo, conflicto.entidadUuid,
                ComparadorConflictos.ciclo(local, remoto),
                remoto.version == null ? 0 : remoto.version,
                reaplicable, remoto.estado);
    }

    private ConflictoLocal conflicto(String tipo, String uuid) {
        ConflictoLocal conflicto = db.conflictoDao().porClave(ConflictoLocal.clave(tipo, uuid));
        if (conflicto == null) {
            throw new IllegalStateException("No se encontró la información del conflicto.");
        }
        return conflicto;
    }

    private void validarAutor(String correo) {
        if (correo == null || !correo.equalsIgnoreCase(sesion.getUsuario())) {
            throw new IllegalStateException(
                    "Solo la cuenta autora puede resolver este cambio desde el teléfono.");
        }
    }

    private String operacionParaReaplicar(String original) {
        if (JornadaLocal.PENDIENTE_ANULAR.equals(original)) {
            return JornadaLocal.PENDIENTE_ANULAR;
        }
        // Un CREATE en conflicto significa que el UUID ya existe en Django.
        // La reaplicación consciente debe convertirse en una corrección versionada.
        return JornadaLocal.PENDIENTE_EDITAR;
    }

    private void resolverListo(AlResolver callback) {
        AppExecutors.enHiloPrincipal(callback::listo);
    }

    private void resolverError(AlResolver callback, Exception error) {
        String mensaje = mensaje(error);
        AppExecutors.enHiloPrincipal(() -> callback.error(mensaje));
    }

    private String mensaje(Exception error) {
        if (error instanceof SQLiteException) {
            return "No se pudo guardar la resolución en el teléfono: " + error.getMessage();
        }
        return error.getMessage() == null ? "No se pudo resolver el conflicto."
                : error.getMessage();
    }
}
