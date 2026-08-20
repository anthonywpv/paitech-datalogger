package ec.edu.espol.paipay.datalogger.sync;

import android.content.Context;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.data.local.entity.ConflictoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.SemaforoLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;
import ec.edu.espol.paipay.datalogger.data.remote.DjangoApiService;
import ec.edu.espol.paipay.datalogger.data.remote.DjangoCliente;
import ec.edu.espol.paipay.datalogger.data.remote.dto.JornadaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CicloApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CierreCicloDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.MovimientoApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.PiscinaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.SemaforoApiDto;
import ec.edu.espol.paipay.datalogger.data.repo.MapeadorApi;
import ec.edu.espol.paipay.datalogger.data.repo.DispositivoManager;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;
import ec.edu.espol.paipay.datalogger.util.RedUtil;
import retrofit2.Response;

/** Cola offline contra Django con UUID idempotente y versiones optimistas. */
public class SincronizacionRepositorio {
    private static class SesionExpiradaException extends Exception {
        SesionExpiradaException() { super("Sesión expirada"); }
    }
    public interface CallbackResumen { void listo(ResumenPendientes resumen); }
    public interface CallbackSincronizacion {
        void progreso(String mensaje);
        void terminado(ResultadoSincronizacion resultado);
    }

    private final Context contexto;
    private final PaipayDatabase db;
    private final SesionManager sesion;
    private final String dispositivoId;
    private final Gson gson = new Gson();

    public SincronizacionRepositorio(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        db = PaipayDatabase.obtener(contexto);
        sesion = SesionManager.obtener(contexto);
        dispositivoId = DispositivoManager.obtener(contexto);
    }

    public void contarPendientes(CallbackResumen callback) {
        AppExecutors.io().execute(() -> {
            int jornadas = 0, movimientos = 0, anulaciones = 0;
            for (JornadaConPeces item : db.jornadaDao().pendientes()) {
                if (JornadaLocal.PENDIENTE_ANULAR.equals(item.jornada.estadoLocal)) anulaciones++;
                else jornadas++;
            }
            for (MovimientoLocal item : db.movimientoDao().pendientes()) {
                if (JornadaLocal.PENDIENTE_ANULAR.equals(item.estadoLocal)) anulaciones++;
                else movimientos++;
            }
            int ciclos = db.cicloDao().pendientes().size();
            ResumenPendientes resumen = new ResumenPendientes(
                    jornadas, movimientos, ciclos, anulaciones);
            AppExecutors.enHiloPrincipal(() -> callback.listo(resumen));
        });
    }

    public boolean hayInternet() { return RedUtil.hayInternet(contexto); }

    public void sincronizar(CallbackSincronizacion callback) {
        if (!hayInternet()) {
            terminar(callback, ResultadoSincronizacion.sinInternet());
            return;
        }
        if (!sesion.haySesionActiva()) {
            terminar(callback, ResultadoSincronizacion.sesionExpirada());
            return;
        }
        AppExecutors.io().execute(() -> ejecutar(callback));
    }

    private void ejecutar(CallbackSincronizacion callback) {
        int subidos = 0;
        int fallidos = 0;
        boolean expirada = false;
        StringBuilder errores = new StringBuilder();
        DjangoApiService api = DjangoCliente.api(contexto);

        List<CicloLocal> ciclos = db.cicloDao().pendientes();
        if (!ciclos.isEmpty()) notificar(callback, "Sincronizando ciclos…");
        for (CicloLocal local : ciclos) {
            if (!mismoAutor(local.autorCorreo)) {
                fallidos++;
                errores.append("autor distinto en ciclo ").append(local.uuid).append("; ");
                continue;
            }
            try {
                boolean crear = CicloLocal.PENDIENTE_CREAR.equals(local.estadoLocal)
                        || CicloLocal.PENDIENTE_CREAR_Y_CERRAR.equals(local.estadoLocal);
                boolean cerrarDespues = CicloLocal.PENDIENTE_CREAR_Y_CERRAR.equals(
                        local.estadoLocal);
                Response<CicloApiDto> respuesta;
                if (crear) {
                    respuesta = api.crearCiclo(
                            MapeadorApi.aDto(local, dispositivoId)).execute();
                    if (respuesta.isSuccessful() && respuesta.body() != null
                            && cerrarDespues) {
                        CicloLocal creado = MapeadorApi.aLocal(
                                respuesta.body(), sesion.getUsuario());
                        creado.cerradoEn = local.cerradoEn;
                        creado.destinoCierre = local.destinoCierre;
                        creado.poblacionFinal = local.poblacionFinal;
                        creado.pesoTotalCosechadoKg = local.pesoTotalCosechadoKg;
                        creado.observacionesCierre = local.observacionesCierre;
                        creado.piscinaDestinoCierreUuid = local.piscinaDestinoCierreUuid;
                        respuesta = api.cerrarCiclo(
                                creado.uuid, MapeadorApi.cierreDto(creado)).execute();
                    }
                } else {
                    respuesta = api.cerrarCiclo(
                            local.uuid, MapeadorApi.cierreDto(local)).execute();
                }
                if (respuesta.code() == 401 || respuesta.code() == 403) {
                    expirada = true;
                    fallidos++;
                } else if (respuesta.code() == 409) {
                    sesion.registrarValidacionServidor(System.currentTimeMillis());
                    registrarConflictoCiclo(api, local);
                    fallidos++;
                    errores.append("conflicto en ciclo ").append(local.uuid).append("; ");
                } else if (respuesta.isSuccessful() && respuesta.body() != null) {
                    sesion.registrarValidacionServidor(System.currentTimeMillis());
                    db.cicloDao().guardar(MapeadorApi.aLocal(
                            respuesta.body(), sesion.getUsuario()));
                    borrarConflicto(ConflictoLocal.CICLO, local.uuid);
                    subidos++;
                } else {
                    local.errorSincronizacion = "HTTP " + respuesta.code();
                    db.cicloDao().guardar(local);
                    fallidos++;
                }
            } catch (Exception error) {
                fallidos++;
                errores.append("ciclo: ").append(error.getMessage()).append("; ");
            }
        }

        List<JornadaConPeces> jornadas = db.jornadaDao().pendientes();
        if (!jornadas.isEmpty()) notificar(callback, "Sincronizando jornadas…");
        for (JornadaConPeces local : jornadas) {
            if (!mismoAutor(local.jornada.autorCorreo)) {
                fallidos++;
                errores.append("autor distinto en jornada ")
                        .append(local.jornada.uuid).append("; ");
                continue;
            }
            try {
                Response<JornadaApiDto> respuesta;
                if (JornadaLocal.PENDIENTE_ANULAR.equals(local.jornada.estadoLocal)) {
                    Map<String, Object> cuerpo = new HashMap<>();
                    cuerpo.put("version", local.jornada.versionServidor);
                    cuerpo.put("motivo", local.jornada.motivoCambio);
                    respuesta = api.anularJornada(local.jornada.uuid, cuerpo).execute();
                } else if (JornadaLocal.PENDIENTE_EDITAR.equals(local.jornada.estadoLocal)) {
                    respuesta = api.editarJornada(local.jornada.uuid,
                            MapeadorApi.aDto(local, dispositivoId)).execute();
                } else {
                    respuesta = api.crearJornada(MapeadorApi.aDto(local, dispositivoId)).execute();
                }
                if (respuesta.code() == 401 || respuesta.code() == 403) {
                    expirada = true;
                    fallidos++;
                    continue;
                }
                if (respuesta.isSuccessful() || respuesta.code() == 409) {
                    sesion.registrarValidacionServidor(System.currentTimeMillis());
                }
                if (respuesta.code() == 409) {
                    registrarConflictoJornada(api, local, local.jornada.estadoLocal);
                    fallidos++;
                    errores.append("conflicto en jornada ").append(local.jornada.uuid).append("; ");
                    continue;
                }
                if (!respuesta.isSuccessful() || respuesta.body() == null) {
                    local.jornada.errorSincronizacion = "HTTP " + respuesta.code();
                    db.jornadaDao().guardar(local.jornada, local.peces);
                    fallidos++;
                    continue;
                }
                guardarJornadaRemota(respuesta.body(), true);
                subidos++;
            } catch (Exception error) {
                fallidos++;
                errores.append("jornada: ").append(error.getMessage()).append("; ");
            }
        }

        List<MovimientoLocal> movimientos = db.movimientoDao().pendientes();
        if (!movimientos.isEmpty()) notificar(callback, "Sincronizando movimientos…");
        for (MovimientoLocal local : movimientos) {
            if (!mismoAutor(local.autorCorreo)) {
                fallidos++;
                errores.append("autor distinto en movimiento ")
                        .append(local.uuid).append("; ");
                continue;
            }
            try {
                MovimientoApiDto cuerpo = MapeadorApi.aDto(local);
                Response<MovimientoApiDto> respuesta;
                if (JornadaLocal.PENDIENTE_ANULAR.equals(local.estadoLocal)) {
                    Map<String, Object> anulacion = new HashMap<>();
                    anulacion.put("version", local.versionServidor);
                    anulacion.put("motivo", local.motivoCambio);
                    respuesta = api.anularMovimiento(local.uuid, anulacion).execute();
                } else if (JornadaLocal.PENDIENTE_EDITAR.equals(local.estadoLocal)) {
                    respuesta = api.editarMovimiento(local.uuid, cuerpo).execute();
                } else {
                    respuesta = api.crearMovimiento(cuerpo).execute();
                }
                if (respuesta.code() == 401 || respuesta.code() == 403) {
                    expirada = true;
                    fallidos++;
                } else if (respuesta.code() == 409) {
                    sesion.registrarValidacionServidor(System.currentTimeMillis());
                    registrarConflictoMovimiento(api, local, local.estadoLocal);
                    fallidos++;
                } else if (respuesta.isSuccessful() && respuesta.body() != null) {
                    sesion.registrarValidacionServidor(System.currentTimeMillis());
                    db.movimientoDao().guardar(MapeadorApi.aLocal(
                            respuesta.body(), sesion.getUsuario()));
                    borrarConflicto(ConflictoLocal.MOVIMIENTO, local.uuid);
                    subidos++;
                } else {
                    fallidos++;
                }
            } catch (Exception error) {
                fallidos++;
                errores.append("movimiento: ").append(error.getMessage()).append("; ");
            }
        }

        if (!expirada) {
            notificar(callback, "Actualizando catálogos y semáforo…");
            try { refrescarDesdeServidor(api); }
            catch (SesionExpiradaException error) { expirada = true; }
            catch (Exception error) { errores.append("refresco: ").append(error.getMessage()).append("; "); }
        }

        if (subidos > 0) sesion.registrarSincronizacion(System.currentTimeMillis());
        ResultadoSincronizacion.Estado estado;
        if (expirada) estado = ResultadoSincronizacion.Estado.SESION_EXPIRADA;
        else if (subidos == 0 && fallidos == 0 && ciclos.isEmpty()
                && jornadas.isEmpty() && movimientos.isEmpty()) estado = ResultadoSincronizacion.Estado.SIN_PENDIENTES;
        else if (fallidos == 0) estado = ResultadoSincronizacion.Estado.EXITO;
        else if (subidos > 0) estado = ResultadoSincronizacion.Estado.PARCIAL;
        else estado = ResultadoSincronizacion.Estado.ERROR;
        terminar(callback, new ResultadoSincronizacion(estado, subidos, fallidos,
                errores.length() == 0 ? null : errores.toString()));
    }

    public void refrescarSoloLectura(CallbackSincronizacion callback) {
        if (!hayInternet()) { terminar(callback, ResultadoSincronizacion.sinInternet()); return; }
        if (!sesion.haySesionActiva()) {
            terminar(callback, ResultadoSincronizacion.sesionExpirada());
            return;
        }
        AppExecutors.io().execute(() -> {
            try {
                refrescarDesdeServidor(DjangoCliente.api(contexto));
                terminar(callback, new ResultadoSincronizacion(
                        ResultadoSincronizacion.Estado.EXITO, 0, 0, null));
            } catch (SesionExpiradaException error) {
                terminar(callback, ResultadoSincronizacion.sesionExpirada());
            } catch (Exception error) {
                terminar(callback, new ResultadoSincronizacion(
                        ResultadoSincronizacion.Estado.ERROR, 0, 1, error.getMessage()));
            }
        });
    }

    private void refrescarDesdeServidor(DjangoApiService api) throws Exception {
        boolean servidorValido = false;
        Response<List<PiscinaApiDto>> piscinas = api.piscinas().execute();
        if (piscinas.isSuccessful() && piscinas.body() != null) {
            servidorValido = true;
            List<PiscinaLocal> locales = new ArrayList<>();
            for (PiscinaApiDto item : piscinas.body()) {
                PiscinaLocal local = MapeadorApi.piscina(item);
                PiscinaLocal anterior = db.catalogoDao().piscina(local.uuid);
                if (anterior != null && anterior.cicloActivoUuid != null) {
                    CicloLocal cicloLocal = db.cicloDao().porUuid(anterior.cicloActivoUuid);
                    if (cicloLocal != null && cicloLocal.pendiente()) {
                        local.cicloActivoUuid = anterior.cicloActivoUuid;
                        local.cicloActivoNumero = anterior.cicloActivoNumero;
                    }
                }
                locales.add(local);
            }
            db.catalogoDao().guardarPiscinas(locales);
        }
        Response<List<CicloApiDto>> ciclos = api.ciclos().execute();
        if (ciclos.code() == 401 || ciclos.code() == 403) {
            throw new SesionExpiradaException();
        }
        if (ciclos.isSuccessful() && ciclos.body() != null) {
            servidorValido = true;
            for (CicloApiDto item : ciclos.body()) {
                String estado = db.cicloDao().estadoLocalDe(item.id);
                if (estado == null || CicloLocal.SINCRONIZADO.equals(estado)) {
                    db.cicloDao().guardar(MapeadorApi.aLocal(item, sesion.getUsuario()));
                }
            }
        }
        Response<List<JornadaApiDto>> jornadas = api.jornadas().execute();
        if (jornadas.code() == 401 || jornadas.code() == 403) {
            throw new SesionExpiradaException();
        }
        if (jornadas.isSuccessful() && jornadas.body() != null) {
            servidorValido = true;
            for (JornadaApiDto item : jornadas.body()) guardarJornadaRemota(item, false);
        }
        Response<List<SemaforoApiDto>> semaforos = api.semaforos().execute();
        if (semaforos.isSuccessful() && semaforos.body() != null) {
            servidorValido = true;
            List<SemaforoLocal> locales = new ArrayList<>();
            for (SemaforoApiDto item : semaforos.body()) locales.add(semaforoLocal(item));
            db.catalogoDao().guardarSemaforos(locales);
        }
        Response<List<MovimientoApiDto>> movimientos = api.movimientos().execute();
        if (movimientos.isSuccessful() && movimientos.body() != null) {
            servidorValido = true;
            for (MovimientoApiDto item : movimientos.body()) {
                String estado = db.movimientoDao().estadoDe(item.id);
                if (estado == null || JornadaLocal.SINCRONIZADO.equals(estado)
                        || JornadaLocal.ANULADO.equals(estado)) {
                    db.movimientoDao().guardar(MapeadorApi.aLocal(item, sesion.getUsuario()));
                }
            }
        }
        if (servidorValido) {
            sesion.registrarValidacionServidor(System.currentTimeMillis());
        }
    }

    private void guardarJornadaRemota(JornadaApiDto dto, boolean forzar) {
        String estado = db.jornadaDao().estadoDe(dto.id);
        if (!forzar && estado != null && !JornadaLocal.SINCRONIZADO.equals(estado)
                && !JornadaLocal.ANULADO.equals(estado)) return;
        PiscinaLocal piscina = db.catalogoDao().piscina(dto.piscina);
        JornadaConPeces local = MapeadorApi.aLocal(dto, piscina, sesion.getUsuario());
        db.jornadaDao().guardar(local.jornada, local.peces);
        if (forzar) borrarConflicto(ConflictoLocal.JORNADA, dto.id);
    }

    private SemaforoLocal semaforoLocal(SemaforoApiDto dto) {
        SemaforoLocal local = new SemaforoLocal();
        local.piscinaUuid = dto.piscina.id;
        local.piscinaCodigo = dto.piscina.codigo;
        local.piscinaNombre = dto.piscina.nombre;
        local.actualizadoEn = System.currentTimeMillis();
        if (dto.jornada != null && dto.jornada.agua != null) {
            local.jornadaUuid = dto.jornada.id;
            local.capturadaEn = dto.jornada.capturadaEn;
            local.autorNombre = dto.jornada.autor == null ? null : dto.jornada.autor.nombre;
            local.ph = Double.valueOf(dto.jornada.agua.ph);
            local.nitrato = Double.valueOf(dto.jornada.agua.nitrato);
            local.nitrito = Double.valueOf(dto.jornada.agua.nitrito);
            local.amoniacoTotal = Double.valueOf(dto.jornada.agua.amoniacoTotal);
            local.estado = dto.semaforo == null ? null : dto.semaforo.estado;
            if (dto.semaforo != null && dto.semaforo.lecturas != null && !dto.semaforo.lecturas.isEmpty()) {
                local.resumen = dto.semaforo.lecturas.get(0).diagnostico;
                for (SemaforoApiDto.LecturaDto lectura : dto.semaforo.lecturas) {
                    if (local.estado != null && local.estado.equals(lectura.estado)) {
                        local.resumen = lectura.diagnostico;
                        break;
                    }
                }
            }
        }
        return local;
    }

    private void registrarConflictoJornada(DjangoApiService api, JornadaConPeces local,
                                            String operacionLocal) {
        ConflictoLocal conflicto = conflictoBase(
                ConflictoLocal.JORNADA, local.jornada.uuid, operacionLocal);
        try {
            Response<JornadaApiDto> actual = api.jornada(local.jornada.uuid).execute();
            if (actual.isSuccessful() && actual.body() != null) {
                conflicto.versionRemota = actual.body().version;
                conflicto.remotoJson = gson.toJson(actual.body());
            }
        } catch (Exception ignorado) {
            // El conflicto se conserva aunque falle la descarga de la instantánea.
            // La pantalla de resolución volverá a solicitarla con Internet.
        }
        local.jornada.estadoLocal = JornadaLocal.CONFLICTO;
        local.jornada.errorSincronizacion = "El servidor tiene una versión más reciente.";
        db.runInTransaction(() -> {
            db.jornadaDao().guardar(local.jornada, local.peces);
            db.conflictoDao().guardar(conflicto);
        });
    }

    private void registrarConflictoMovimiento(DjangoApiService api, MovimientoLocal local,
                                               String operacionLocal) {
        ConflictoLocal conflicto = conflictoBase(
                ConflictoLocal.MOVIMIENTO, local.uuid, operacionLocal);
        try {
            Response<MovimientoApiDto> actual = api.movimiento(local.uuid).execute();
            if (actual.isSuccessful() && actual.body() != null) {
                conflicto.versionRemota = actual.body().version;
                conflicto.remotoJson = gson.toJson(actual.body());
            }
        } catch (Exception ignorado) {
            // Se mantiene la copia local; la instantánea se puede recuperar después.
        }
        local.estadoLocal = JornadaLocal.CONFLICTO;
        local.errorSincronizacion = "El servidor tiene una versión más reciente.";
        db.runInTransaction(() -> {
            db.movimientoDao().guardar(local);
            db.conflictoDao().guardar(conflicto);
        });
    }

    private void registrarConflictoCiclo(DjangoApiService api, CicloLocal local) {
        ConflictoLocal conflicto = conflictoBase(
                ConflictoLocal.CICLO, local.uuid, local.estadoLocal);
        try {
            Response<CicloApiDto> actual = api.ciclo(local.uuid).execute();
            if (actual.isSuccessful() && actual.body() != null) {
                conflicto.versionRemota = actual.body().version;
                conflicto.remotoJson = gson.toJson(actual.body());
            }
        } catch (Exception ignorado) {
            // Puede ser un 409 por otro ciclo activo con UUID distinto. El cambio
            // local se conserva y la piscina remota se verá tras el refresco.
        }
        local.estadoLocal = CicloLocal.CONFLICTO;
        local.errorSincronizacion = "Existe un ciclo o una versión incompatible en el servidor.";
        db.runInTransaction(() -> {
            db.cicloDao().guardar(local);
            db.conflictoDao().guardar(conflicto);
        });
    }

    private ConflictoLocal conflictoBase(String tipo, String uuid, String operacionLocal) {
        ConflictoLocal conflicto = new ConflictoLocal();
        conflicto.clave = ConflictoLocal.clave(tipo, uuid);
        conflicto.tipo = tipo;
        conflicto.entidadUuid = uuid;
        conflicto.operacionLocal = operacionLocal;
        conflicto.detectadoEn = System.currentTimeMillis();
        return conflicto;
    }

    private void borrarConflicto(String tipo, String uuid) {
        db.conflictoDao().borrar(ConflictoLocal.clave(tipo, uuid));
    }

    private void notificar(CallbackSincronizacion callback, String mensaje) {
        AppExecutors.enHiloPrincipal(() -> callback.progreso(mensaje));
    }
    private boolean mismoAutor(String autorCorreo) {
        return autorCorreo != null
                && autorCorreo.equalsIgnoreCase(sesion.getUsuario());
    }
    private void terminar(CallbackSincronizacion callback, ResultadoSincronizacion resultado) {
        AppExecutors.enHiloPrincipal(() -> callback.terminado(resultado));
    }
}
