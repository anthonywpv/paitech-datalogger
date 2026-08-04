package ec.edu.espol.paipay.datalogger.data.remote;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Endpoints de Neon Auth (Managed Better Auth) usados por la app.
 *
 * Flujo:
 *   1. sign-in/email  → valida usuario y contraseña, devuelve cookie de sesión.
 *   2. get-session    → con esa cookie devuelve un JWT fresco en la cabecera
 *                       "set-auth-jwt". Ese JWT es el que la Data API exige.
 *
 * Los JWT de Neon caducan en unos 15 minutos, por eso NUNCA se guardan a largo
 * plazo: lo que se guarda es la cookie de sesión, y el JWT se pide cuando hace
 * falta. Así el productor inicia sesión una sola vez.
 */
public interface NeonAuthService {

    @POST("sign-in/email")
    Call<Map<String, Object>> iniciarSesion(@Body Map<String, String> credenciales);

    @GET("get-session")
    Call<Map<String, Object>> obtenerSesion();
}
