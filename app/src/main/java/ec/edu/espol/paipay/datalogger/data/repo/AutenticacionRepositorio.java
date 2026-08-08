package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;

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
        void onExito(String nombre);
        void onError(Resultado resultado);
    }

    public enum Resultado {
        EXITO, CREDENCIALES_INVALIDAS, DATOS_DE_OTRA_CUENTA, RECHAZADO_POR_SERVIDOR,
        SIN_INTERNET, ERROR_SERVIDOR
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
                sesion.cerrarSesion();
                DjangoCliente.reiniciar();
                Map<String, String> credenciales = new HashMap<>();
                credenciales.put("email", correoNormalizado);
                credenciales.put("password", clave);
                Response<LoginRespuestaDto> respuesta = DjangoCliente.api(contexto)
                        .iniciarSesion(credenciales).execute();
                if (respuesta.code() == 400 || respuesta.code() == 401) {
                    informar(callback, Resultado.CREDENCIALES_INVALIDAS);
                    return;
                }
                if (!respuesta.isSuccessful() || respuesta.body() == null
                        || respuesta.body().usuario == null) {
                    informar(callback, Resultado.ERROR_SERVIDOR);
                    return;
                }
                LoginRespuestaDto cuerpo = respuesta.body();
                sesion.guardarSesion(cuerpo.usuario.correo, cuerpo.usuario.nombre, cuerpo.token);
                DjangoCliente.reiniciar();

                // El primer login también debe dejar el catálogo listo para usar offline.
                Response<List<PiscinaApiDto>> catalogo = DjangoCliente.api(contexto)
                        .piscinas().execute();
                if (!catalogo.isSuccessful() || catalogo.body() == null) {
                    sesion.cerrarSesion();
                    DjangoCliente.reiniciar();
                    informar(callback, Resultado.ERROR_SERVIDOR);
                    return;
                }
                List<PiscinaLocal> locales = new ArrayList<>();
                for (PiscinaApiDto piscina : catalogo.body()) locales.add(MapeadorApi.piscina(piscina));
                db.catalogoDao().guardarPiscinas(locales);
                AppExecutors.enHiloPrincipal(() -> callback.onExito(cuerpo.usuario.nombre));
            } catch (Exception error) {
                informar(callback, Resultado.ERROR_SERVIDOR);
            }
        });
    }

    private void informar(Callback callback, Resultado resultado) {
        AppExecutors.enHiloPrincipal(() -> callback.onError(resultado));
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

}
