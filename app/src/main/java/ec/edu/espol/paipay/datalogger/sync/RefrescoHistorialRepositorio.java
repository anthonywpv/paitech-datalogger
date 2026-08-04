package ec.edu.espol.paipay.datalogger.sync;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.data.local.entity.EnsayoLaboratorio;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;
import ec.edu.espol.paipay.datalogger.data.remote.NeonApiService;
import ec.edu.espol.paipay.datalogger.data.remote.NeonCliente;
import ec.edu.espol.paipay.datalogger.data.remote.dto.AguaDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.BiometriaDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.LaboratorioDto;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;
import ec.edu.espol.paipay.datalogger.util.RedUtil;
import retrofit2.Response;

/**
 * ===========================================================================
 *  REFRESCO DEL HISTORIAL — traer de la base principal lo propio
 * ===========================================================================
 *
 * Baja de Neon los registros hechos con el correo de la sesión y los mete en
 * SQLite. Sirve para que el productor vea también lo que registró desde otro
 * teléfono, o lo que quedó en el servidor tras reinstalar la app.
 *
 * Es un COMPLEMENTO, no la fuente del historial. La lista se pinta siempre
 * desde Room: en Paipayales la señal es intermitente y un historial que
 * dependiera de la red aparecería vacío justo cuando el productor está parado
 * en el borde de la piscina comprobando si guardó bien el dato.
 *
 * REGLA DE FUSIÓN: LO LOCAL SIEMPRE GANA.
 * Solo se insertan los uuid que el teléfono no tiene. Nunca se sobrescribe una
 * fila existente, porque podría llevar una corrección todavía sin subir y el
 * servidor devolvería la versión vieja, deshaciendo el trabajo del productor.
 */
public class RefrescoHistorialRepositorio {

    /** Orden que se le pide a PostgREST: lo más reciente primero. */
    private static final String ORDEN = "creado_en.desc";

    public enum Estado { OK, SIN_INTERNET, SIN_SESION, ERROR }

    public static class Resultado {
        public final Estado estado;
        /** Registros nuevos que se incorporaron al teléfono. */
        public final int traidos;

        Resultado(Estado estado, int traidos) {
            this.estado = estado;
            this.traidos = traidos;
        }
    }

    public interface Callback {
        void terminado(Resultado resultado);
    }

    private final Context contexto;
    private final PaipayDatabase db;
    private final SesionManager sesion;

    public RefrescoHistorialRepositorio(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.db = PaipayDatabase.obtener(contexto);
        this.sesion = SesionManager.obtener(contexto);
    }

    public void refrescar(Callback callback) {
        final String correo = sesion.getUsuario();
        if (correo == null || correo.isEmpty()) {
            responder(callback, new Resultado(Estado.SIN_SESION, 0));
            return;
        }
        if (!RedUtil.hayInternet(contexto)) {
            responder(callback, new Resultado(Estado.SIN_INTERNET, 0));
            return;
        }

        AppExecutors.io().execute(() -> {
            // Sintaxis de filtro de PostgREST.
            final String filtro = "eq." + correo;
            NeonApiService api = NeonCliente.datos(contexto);
            int traidos = 0;

            try {
                traidos += fusionarBiometria(api.listarBiometria(filtro, ORDEN).execute());
                traidos += fusionarAgua(api.listarAgua(filtro, ORDEN).execute());
                traidos += fusionarLaboratorio(api.listarLaboratorio(filtro, ORDEN).execute());
            } catch (Exception e) {
                responder(callback, new Resultado(Estado.ERROR, traidos));
                return;
            }

            responder(callback, new Resultado(Estado.OK, traidos));
        });
    }

    // ------------------------------------------------------------------

    private int fusionarBiometria(Response<List<BiometriaDto>> respuesta) {
        if (!respuesta.isSuccessful() || respuesta.body() == null) return 0;

        Set<String> yaLocales = new HashSet<>(db.biometriaDao().todosLosUuids());
        List<RegistroBiometria> nuevos = new ArrayList<>();
        for (BiometriaDto d : respuesta.body()) {
            if (d.uuid == null || yaLocales.contains(d.uuid)) continue;
            nuevos.add(d.aEntidad());
        }
        if (nuevos.isEmpty()) return 0;
        db.biometriaDao().insertarOReemplazar(nuevos);
        return nuevos.size();
    }

    private int fusionarAgua(Response<List<AguaDto>> respuesta) {
        if (!respuesta.isSuccessful() || respuesta.body() == null) return 0;

        Set<String> yaLocales = new HashSet<>(db.aguaDao().todosLosUuids());
        List<RegistroAgua> nuevos = new ArrayList<>();
        for (AguaDto d : respuesta.body()) {
            if (d.uuid == null || yaLocales.contains(d.uuid)) continue;
            nuevos.add(d.aEntidad());
        }
        if (nuevos.isEmpty()) return 0;
        db.aguaDao().insertarOReemplazar(nuevos);
        return nuevos.size();
    }

    private int fusionarLaboratorio(Response<List<LaboratorioDto>> respuesta) {
        if (!respuesta.isSuccessful() || respuesta.body() == null) return 0;

        Set<String> yaLocales = new HashSet<>(db.laboratorioDao().todosLosUuids());
        List<EnsayoLaboratorio> nuevos = new ArrayList<>();
        for (LaboratorioDto d : respuesta.body()) {
            if (d.uuid == null || yaLocales.contains(d.uuid)) continue;
            nuevos.add(d.aEntidad());
        }
        if (nuevos.isEmpty()) return 0;
        db.laboratorioDao().insertarOReemplazar(nuevos);
        return nuevos.size();
    }

    private void responder(Callback callback, Resultado resultado) {
        AppExecutors.enHiloPrincipal(() -> callback.terminado(resultado));
    }
}
