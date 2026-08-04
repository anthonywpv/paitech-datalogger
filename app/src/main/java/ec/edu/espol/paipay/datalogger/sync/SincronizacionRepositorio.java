package ec.edu.espol.paipay.datalogger.sync;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.data.local.entity.EnsayoLaboratorio;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;
import ec.edu.espol.paipay.datalogger.data.remote.NeonApiService;
import ec.edu.espol.paipay.datalogger.data.remote.NeonCliente;
import ec.edu.espol.paipay.datalogger.data.remote.SesionExpiradaException;
import ec.edu.espol.paipay.datalogger.data.remote.dto.AguaDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.BiometriaDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.LaboratorioDto;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;
import ec.edu.espol.paipay.datalogger.util.RedUtil;
import retrofit2.Response;

/**
 * ===========================================================================
 *  MOTOR DE SINCRONIZACIÓN DIFERIDA
 * ===========================================================================
 *
 * Flujo completo:
 *
 *   1. El productor registra datos en campo (sin internet). Todo va a SQLite
 *      con sincronizado = 0.
 *   2. Al llegar a un punto con señal, pulsa [Sincronizar].
 *   3. La app le muestra QUÉ va a subir y le pide confirmación explícita.
 *   4. Se suben los registros por lotes a la Neon Data API.
 *   5. Solo cuando el servidor responde 2xx se marca sincronizado = 1 local.
 *
 * Garantías de diseño:
 *
 *   · NUNCA se borra un dato local. Sincronizar es copiar, no mover.
 *   · La subida es IDEMPOTENTE gracias al UPSERT sobre la columna uuid:
 *     repetir la sincronización no duplica filas en la base principal.
 *   · Si un lote falla, los otros dos siguen intentándose (resultado PARCIAL).
 *   · Todo corre fuera del hilo principal.
 */
public class SincronizacionRepositorio {

    /** Tamaño de lote: suficientemente chico para sobrevivir a una señal débil. */
    private static final int TAMANO_LOTE = 50;

    public interface CallbackResumen {
        void listo(ResumenPendientes resumen);
    }

    public interface CallbackSincronizacion {
        void progreso(String mensaje);
        void terminado(ResultadoSincronizacion resultado);
    }

    private final Context contexto;
    private final PaipayDatabase db;
    private final SesionManager sesion;

    public SincronizacionRepositorio(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.db = PaipayDatabase.obtener(contexto);
        this.sesion = SesionManager.obtener(contexto);
    }

    // ------------------------------------------------------------------
    //  PASO PREVIO: ¿qué hay pendiente?
    // ------------------------------------------------------------------

    public void contarPendientes(CallbackResumen callback) {
        AppExecutors.io().execute(() -> {
            ResumenPendientes resumen = new ResumenPendientes(
                    db.biometriaDao().contarPendientes(),
                    db.aguaDao().contarPendientes(),
                    db.laboratorioDao().contarPendientes());
            AppExecutors.enHiloPrincipal(() -> callback.listo(resumen));
        });
    }

    public boolean hayInternet() {
        return RedUtil.hayInternet(contexto);
    }

    // ------------------------------------------------------------------
    //  SINCRONIZACIÓN
    // ------------------------------------------------------------------

    public void sincronizar(CallbackSincronizacion callback) {

        if (!RedUtil.hayInternet(contexto)) {
            AppExecutors.enHiloPrincipal(() ->
                    callback.terminado(ResultadoSincronizacion.sinInternet()));
            return;
        }

        AppExecutors.io().execute(() -> {
            int subidos = 0;
            int fallidos = 0;
            boolean sesionExpirada = false;
            StringBuilder errores = new StringBuilder();
            long momento = System.currentTimeMillis();

            NeonApiService api = NeonCliente.datos(contexto);

            // ---------- 1. BIOMETRÍA ----------
            List<RegistroBiometria> biometrias = db.biometriaDao().pendientes();
            if (!biometrias.isEmpty()) {
                notificar(callback, "Subiendo biometría de peces…");
                for (List<RegistroBiometria> lote : dividir(biometrias, TAMANO_LOTE)) {
                    List<BiometriaDto> cuerpo = new ArrayList<>(lote.size());
                    List<String> uuids = new ArrayList<>(lote.size());
                    for (RegistroBiometria r : lote) {
                        cuerpo.add(BiometriaDto.desde(r));
                        uuids.add(r.uuid);
                    }
                    try {
                        Response<Void> resp = api
                                .subirBiometria(NeonApiService.PREFER_UPSERT, cuerpo)
                                .execute();
                        if (resp.isSuccessful()) {
                            db.biometriaDao().marcarSincronizados(uuids, momento);
                            subidos += lote.size();
                        } else {
                            fallidos += lote.size();
                            errores.append("biometría HTTP ").append(resp.code()).append("; ");
                        }
                    } catch (SesionExpiradaException e) {
                        sesionExpirada = true;
                        fallidos += lote.size();
                        errores.append("biometría: sesión expirada; ");
                    } catch (Exception e) {
                        fallidos += lote.size();
                        errores.append("biometría: ").append(e.getMessage()).append("; ");
                    }
                }
            }

            // ---------- 2. CALIDAD DE AGUA ----------
            List<RegistroAgua> aguas = db.aguaDao().pendientes();
            if (!aguas.isEmpty()) {
                notificar(callback, "Subiendo mediciones de agua…");
                for (List<RegistroAgua> lote : dividir(aguas, TAMANO_LOTE)) {
                    List<AguaDto> cuerpo = new ArrayList<>(lote.size());
                    List<String> uuids = new ArrayList<>(lote.size());
                    for (RegistroAgua r : lote) {
                        cuerpo.add(AguaDto.desde(r));
                        uuids.add(r.uuid);
                    }
                    try {
                        Response<Void> resp = api
                                .subirAgua(NeonApiService.PREFER_UPSERT, cuerpo)
                                .execute();
                        if (resp.isSuccessful()) {
                            db.aguaDao().marcarSincronizados(uuids, momento);
                            subidos += lote.size();
                        } else {
                            fallidos += lote.size();
                            errores.append("agua HTTP ").append(resp.code()).append("; ");
                        }
                    } catch (SesionExpiradaException e) {
                        sesionExpirada = true;
                        fallidos += lote.size();
                        errores.append("agua: sesión expirada; ");
                    } catch (Exception e) {
                        fallidos += lote.size();
                        errores.append("agua: ").append(e.getMessage()).append("; ");
                    }
                }
            }

            // ---------- 3. ENSAYOS DE LABORATORIO ----------
            List<EnsayoLaboratorio> ensayos = db.laboratorioDao().pendientes();
            if (!ensayos.isEmpty()) {
                notificar(callback, "Subiendo ensayos de laboratorio…");
                for (List<EnsayoLaboratorio> lote : dividir(ensayos, TAMANO_LOTE)) {
                    List<LaboratorioDto> cuerpo = new ArrayList<>(lote.size());
                    List<String> uuids = new ArrayList<>(lote.size());
                    for (EnsayoLaboratorio e : lote) {
                        cuerpo.add(LaboratorioDto.desde(e));
                        uuids.add(e.uuid);
                    }
                    try {
                        Response<Void> resp = api
                                .subirLaboratorio(NeonApiService.PREFER_UPSERT, cuerpo)
                                .execute();
                        if (resp.isSuccessful()) {
                            db.laboratorioDao().marcarSincronizados(uuids, momento);
                            subidos += lote.size();
                        } else {
                            fallidos += lote.size();
                            errores.append("laboratorio HTTP ").append(resp.code()).append("; ");
                        }
                    } catch (SesionExpiradaException e) {
                        sesionExpirada = true;
                        fallidos += lote.size();
                        errores.append("laboratorio: sesión expirada; ");
                    } catch (Exception e) {
                        fallidos += lote.size();
                        errores.append("laboratorio: ").append(e.getMessage()).append("; ");
                    }
                }
            }

            // ---------- Resultado ----------
            final int fSubidos = subidos;
            final int fFallidos = fallidos;
            final String fErrores = errores.length() == 0 ? null : errores.toString();

            if (fSubidos == 0 && fFallidos == 0) {
                AppExecutors.enHiloPrincipal(() ->
                        callback.terminado(ResultadoSincronizacion.sinPendientes()));
                return;
            }

            if (fSubidos > 0) {
                sesion.registrarSincronizacion(momento);
            }

            ResultadoSincronizacion.Estado estado;
            if (sesionExpirada) {
                // Prioritario sobre PARCIAL: aunque algún lote haya subido, lo que
                // el productor necesita saber es que tiene que volver a entrar.
                estado = ResultadoSincronizacion.Estado.SESION_EXPIRADA;
            } else if (fFallidos == 0) {
                estado = ResultadoSincronizacion.Estado.EXITO;
            } else if (fSubidos > 0) {
                estado = ResultadoSincronizacion.Estado.PARCIAL;
            } else {
                estado = ResultadoSincronizacion.Estado.ERROR;
            }

            ResultadoSincronizacion resultado =
                    new ResultadoSincronizacion(estado, fSubidos, fFallidos, fErrores);
            AppExecutors.enHiloPrincipal(() -> callback.terminado(resultado));
        });
    }

    // ------------------------------------------------------------------

    private void notificar(CallbackSincronizacion callback, String mensaje) {
        AppExecutors.enHiloPrincipal(() -> callback.progreso(mensaje));
    }

    /** Parte una lista grande en sublistas del tamaño indicado. */
    private static <T> List<List<T>> dividir(List<T> origen, int tamano) {
        List<List<T>> lotes = new ArrayList<>();
        for (int i = 0; i < origen.size(); i += tamano) {
            lotes.add(new ArrayList<>(origen.subList(i, Math.min(origen.size(), i + tamano))));
        }
        return lotes;
    }
}
