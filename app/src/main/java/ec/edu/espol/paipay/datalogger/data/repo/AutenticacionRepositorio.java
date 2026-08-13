package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;
import android.os.Build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ec.edu.espol.paipay.datalogger.data.local.PaipayDatabase;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.remote.DjangoCliente;
import ec.edu.espol.paipay.datalogger.data.remote.dto.LoginRespuestaDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.PiscinaApiDto;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;
import ec.edu.espol.paipay.datalogger.util.RedUtil;
import retrofit2.Response;

/** Primer ingreso online contra Django; luego la identidad queda cifrada para trabajar offline. */
public class AutenticacionRepositorio {
    public interface Callback {
        void onExito(String nombre, boolean requiereCambioClave);
        void onError(Resultado resultado);
    }

    public interface CambioClaveCallback {
        void onExito();
        void onError(String mensaje);
    }

    public enum Resultado {
        EXITO, CREDENCIALES_INVALIDAS, DATOS_DE_OTRA_CUENTA, RECHAZADO_POR_SERVIDOR,
        SIN_INTERNET, CUENTA_BLOQUEADA, ERROR_SERVIDOR
    }

    private final Context contexto;
    private final SesionManager sesion;
    private final PaipayDatabase db;

    public AutenticacionRepositorio(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        sesion = SesionManager.obtener(contexto);
        db = PaipayDatabase.obtener(contexto);
    }

    public void iniciarSesion(String correo, String clave, Callback callback) {
        iniciarSesion(correo, clave, true, callback);
    }

    public void iniciarSesion(String correo, String clave, boolean ignorado, Callback callback) {
        if (!RedUtil.hayInternet(contexto)) {
            AppExecutors.enHiloPrincipal(() -> callback.onError(Resultado.SIN_INTERNET));
            return;
        }
        AppExecutors.io().execute(() -> {
            try {
                String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
                if (hayDatosNoResueltosDeOtraCuenta(correoNormalizado)) {
                    informar(callback, Resultado.DATOS_DE_OTRA_CUENTA);
                    return;
                }
                String correoAnterior = sesion.getPropietarioLocal();
                if (correoAnterior.isEmpty()) correoAnterior = sesion.getUsuario();
                sesion.cerrarSesion();
                DjangoCliente.reiniciar();
                Map<String, String> credenciales = new HashMap<>();
                credenciales.put("email", correoNormalizado);
                credenciales.put("password", clave);
                credenciales.put("dispositivo_id", DispositivoManager.obtener(contexto));
                credenciales.put("nombre_dispositivo", Build.MANUFACTURER + " " + Build.MODEL);
                Response<LoginRespuestaDto> respuesta = DjangoCliente.api(contexto)
                        .iniciarSesion(credenciales).execute();
                if (respuesta.code() == 400 || respuesta.code() == 401) {
                    informar(callback, Resultado.CREDENCIALES_INVALIDAS);
                    return;
                }
                if (respuesta.code() == 429) {
                    informar(callback, Resultado.CUENTA_BLOQUEADA);
                    return;
                }
                if (!respuesta.isSuccessful() || respuesta.body() == null
                        || respuesta.body().usuario == null) {
                    informar(callback, Resultado.ERROR_SERVIDOR);
                    return;
                }
                LoginRespuestaDto cuerpo = respuesta.body();
                sesion.guardarSesion(
                        cuerpo.usuario.correo,
                        cuerpo.usuario.nombre,
                        cuerpo.token,
                        cuerpo.debe_cambiar_clave);
                DjangoCliente.reiniciar();

                if (!correoAnterior.isEmpty()
                        && !correoAnterior.equalsIgnoreCase(correoNormalizado)) {
                    db.clearAllTables();
                }

                if (cuerpo.debe_cambiar_clave) {
                    AppExecutors.enHiloPrincipal(() ->
                            callback.onExito(cuerpo.usuario.nombre, true));
                    return;
                }

                // El primer login también debe dejar el catálogo listo para usar offline.
                Response<List<PiscinaApiDto>> catalogo = DjangoCliente.api(contexto)
                        .piscinas().execute();
                if (!catalogo.isSuccessful() || catalogo.body() == null) {
                    limpiarLoginIncompleto(cuerpo.token);
                    informar(callback, Resultado.ERROR_SERVIDOR);
                    return;
                }
                List<PiscinaLocal> locales = new ArrayList<>();
                for (PiscinaApiDto piscina : catalogo.body()) locales.add(MapeadorApi.piscina(piscina));
                db.catalogoDao().guardarPiscinas(locales);
                AppExecutors.enHiloPrincipal(() -> callback.onExito(cuerpo.usuario.nombre, false));
            } catch (Exception error) {
                String tokenIncompleto = sesion.getToken();
                if (!tokenIncompleto.isEmpty()) limpiarLoginIncompleto(tokenIncompleto);
                informar(callback, Resultado.ERROR_SERVIDOR);
            }
        });
    }

    private void informar(Callback callback, Resultado resultado) {
        AppExecutors.enHiloPrincipal(() -> callback.onError(resultado));
    }

    private void limpiarLoginIncompleto(String token) {
        sesion.cerrarSesion();
        DjangoCliente.reiniciar();
        if (token == null || token.isEmpty()) return;
        try { DjangoCliente.api(contexto).cerrarSesion("Token " + token).execute(); }
        catch (Exception ignorada) { }
        DjangoCliente.reiniciar();
    }

    private boolean hayDatosNoResueltosDeOtraCuenta(String correo) {
        for (String autor : db.jornadaDao().autoresNoResueltos()) {
            if (autor != null && !autor.equalsIgnoreCase(correo)) return true;
        }
        for (String autor : db.movimientoDao().autoresNoResueltos()) {
            if (autor != null && !autor.equalsIgnoreCase(correo)) return true;
        }
        return false;
    }

    public void cerrarSesion() {
        String token = sesion.getToken();
        sesion.cerrarSesion();
        DjangoCliente.reiniciar();
        AppExecutors.io().execute(() -> {
            try { DjangoCliente.api(contexto).cerrarSesion("Token " + token).execute(); }
            catch (Exception ignorada) { }
            DjangoCliente.reiniciar();
        });
    }

    public void cerrarSesionCompleta(Runnable alTerminar) {
        String token = sesion.getToken();
        sesion.cerrarSesionCompletaLocal();
        DjangoCliente.reiniciar();
        AppExecutors.io().execute(() -> {
            db.clearAllTables();
            AppExecutors.enHiloPrincipal(alTerminar);
        });
        AppExecutors.io().execute(() -> {
            if (token == null || token.isEmpty()) return;
            try { DjangoCliente.api(contexto).cerrarSesion("Token " + token).execute(); }
            catch (Exception ignorada) { }
            DjangoCliente.reiniciar();
        });
    }

    public void cambiarClave(String actual, String nueva, String confirmacion,
                             CambioClaveCallback callback) {
        if (!RedUtil.hayInternet(contexto)) {
            AppExecutors.enHiloPrincipal(() ->
                    callback.onError("Necesitas internet para cambiar la contraseña."));
            return;
        }
        AppExecutors.io().execute(() -> {
            try {
                Map<String, String> datos = new HashMap<>();
                datos.put("password_actual", actual);
                datos.put("password_nuevo", nueva);
                datos.put("confirmacion", confirmacion);
                Response<Void> respuesta = DjangoCliente.api(contexto).cambiarClave(datos).execute();
                if (respuesta.isSuccessful()) {
                    sesion.cerrarSesion();
                    DjangoCliente.reiniciar();
                    AppExecutors.enHiloPrincipal(callback::onExito);
                    return;
                }
                String mensaje = respuesta.code() == 400
                        ? "La contraseña no cumple la política o los datos no coinciden."
                        : "No se pudo cambiar la contraseña. Inténtalo nuevamente.";
                AppExecutors.enHiloPrincipal(() -> callback.onError(mensaje));
            } catch (Exception error) {
                AppExecutors.enHiloPrincipal(() ->
                        callback.onError("No se pudo contactar al servidor."));
            }
        });
    }

}
