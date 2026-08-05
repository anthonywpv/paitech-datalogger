package ec.edu.espol.paipay.datalogger.data.repo;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import ec.edu.espol.paipay.datalogger.data.remote.NeonCliente;
import ec.edu.espol.paipay.datalogger.data.remote.TokenManager;
import ec.edu.espol.paipay.datalogger.util.AppExecutors;
import ec.edu.espol.paipay.datalogger.util.RedUtil;
import retrofit2.Response;

/**
 * Inicio de sesión contra Neon Auth (Managed Better Auth).
 *
 * SOLO se necesita internet en el PRIMER ingreso:
 *   · sign-in/email devuelve una cookie de sesión de larga duración.
 *   · Esa cookie queda cifrada en el teléfono (CookieJarPersistente).
 *   · A partir de ahí la app abre directo al formulario, incluso sin señal,
 *     y solo pide un JWT nuevo cuando toca sincronizar.
 */
public class AutenticacionRepositorio {

    public interface Callback {
        void onExito(String nombre);
        void onError(Resultado resultado);
    }

    public enum Resultado {
        EXITO,
        CREDENCIALES_INVALIDAS,
        /** El servidor rechazó la petición, no la contraseña. Típicamente el
         *  Origin no está entre los confiables de Neon Auth. */
        RECHAZADO_POR_SERVIDOR,
        SIN_INTERNET,
        ERROR_SERVIDOR
    }

    private final Context contexto;
    private final SesionManager sesion;

    public AutenticacionRepositorio(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.sesion = SesionManager.obtener(contexto);
    }

    public void iniciarSesion(String usuario, String clave, Callback callback) {
        iniciarSesion(usuario, clave, false, callback);
    }

    /**
     * @param recordar si el productor pidió que no se le vuelva a preguntar
     *                 durante 30 días. Sin marcar, la sesión solo vive lo que
     *                 dure la subida en curso.
     */
    public void iniciarSesion(String usuario, String clave, boolean recordar,
                              Callback callback) {
        if (!RedUtil.hayInternet(contexto)) {
            AppExecutors.enHiloPrincipal(() -> callback.onError(Resultado.SIN_INTERNET));
            return;
        }

        AppExecutors.io().execute(() -> {
            try {
                // Se entra de cero: fuera cualquier cookie que haya sobrevivido.
                // Better Auth devuelve 403 si el sign-in llega con la cookie de
                // una sesión anterior, y el mensaje resultante culpa a la
                // contraseña. Limpiar aquí hace que el ingreso funcione sin
                // importar cómo quedó el estado previo.
                NeonCliente.reiniciar(contexto);

                Map<String, String> credenciales = new HashMap<>();
                credenciales.put("email", usuario.trim().toLowerCase());
                credenciales.put("password", clave);

                Response<Map<String, Object>> respuesta = NeonCliente.auth(contexto)
                        .iniciarSesion(credenciales)
                        .execute();

                // 403 NO es contraseña incorrecta: Better Auth lo devuelve cuando
                // rechaza la petición entera, casi siempre porque el Origin no
                // está declarado como confiable en la consola de Neon. Meterlo
                // en el mismo saco que el 401 hace perder horas buscando en el
                // sitio equivocado.
                if (respuesta.code() == 403) {
                    AppExecutors.enHiloPrincipal(() ->
                            callback.onError(Resultado.RECHAZADO_POR_SERVIDOR));
                    return;
                }
                if (respuesta.code() == 401 || respuesta.code() == 400) {
                    AppExecutors.enHiloPrincipal(() ->
                            callback.onError(Resultado.CREDENCIALES_INVALIDAS));
                    return;
                }
                if (!respuesta.isSuccessful()) {
                    AppExecutors.enHiloPrincipal(() -> callback.onError(Resultado.ERROR_SERVIDOR));
                    return;
                }

                // La cookie de sesión ya quedó guardada por el CookieJar.
                // Se comprueba que sirva pidiendo un JWT de inmediato.
                String jwt = TokenManager.obtener(contexto).jwtValido();
                if (jwt == null) {
                    AppExecutors.enHiloPrincipal(() -> callback.onError(Resultado.ERROR_SERVIDOR));
                    return;
                }

                String nombre = extraerNombre(respuesta.body(), usuario);
                sesion.guardarSesion(usuario.trim().toLowerCase(), nombre, recordar);
                AppExecutors.enHiloPrincipal(() -> callback.onExito(nombre));

            } catch (Exception e) {
                AppExecutors.enHiloPrincipal(() -> callback.onError(Resultado.ERROR_SERVIDOR));
            }
        });
    }

    /** Better Auth responde {"user": {"name": "...", "email": "..."} , ...}. */
    @SuppressWarnings("unchecked")
    private String extraerNombre(Map<String, Object> cuerpo, String porDefecto) {
        try {
            if (cuerpo != null && cuerpo.get("user") instanceof Map) {
                Object nombre = ((Map<String, Object>) cuerpo.get("user")).get("name");
                if (nombre instanceof String && !((String) nombre).isEmpty()) {
                    return (String) nombre;
                }
            }
        } catch (Exception ignorada) {
            // Si el formato cambia, se usa el usuario como nombre visible.
        }
        return porDefecto;
    }

    /** Cierra sesión: borra cookie, JWT y datos de perfil. Los registros locales se conservan. */
    public void cerrarSesion() {
        NeonCliente.reiniciar(contexto);
        sesion.cerrarSesion();
    }

    /**
     * Se llama al terminar una sincronización. Si el productor no pidió que se
     * recordara su sesión, la olvida por completo.
     *
     * TIENE que pasar por cerrarSesion() y no por SesionManager a secas: borrar
     * solo las preferencias deja viva la COOKIE de Neon Auth, y Better Auth
     * responde 403 a un sign-in que llega con una cookie de sesión anterior.
     * El síntoma sería desconcertante: la primera subida funciona y la segunda
     * dice "usuario o contraseña incorrectos" con las credenciales correctas.
     */
    public void olvidarSiNoSeRecuerda() {
        if (!sesion.recordarSesion()) cerrarSesion();
    }
}
