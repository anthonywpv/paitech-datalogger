package ec.edu.espol.paipay.datalogger.data.remote;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.remote.dto.AguaDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.BiometriaDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.LaboratorioDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.PiscinaDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Contrato REST contra la Neon Data API (PostgREST sobre Postgres).
 *
 * Ventaja frente a JDBC directo desde el teléfono: la contraseña de Postgres
 * nunca llega al APK, el tráfico va por HTTPS y Neon aplica Row Level Security
 * del lado del servidor usando el JWT del productor.
 *
 * Sobre "Prefer: resolution=merge-duplicates": convierte el INSERT en UPSERT
 * sobre la columna uuid. Esto hace la sincronización IDEMPOTENTE: si el
 * productor pulsa [Sincronizar] dos veces, o si la señal se corta a mitad de
 * la subida, no se duplican filas en la base principal.
 */
public interface NeonApiService {

    String PREFER_UPSERT = "resolution=merge-duplicates,return=minimal";

    // ------------------- CATÁLOGOS -------------------

    /** activaEq usa la sintaxis de PostgREST: "eq.true". */
    @GET("piscina")
    Call<List<PiscinaDto>> listarPiscinas(@Query("activa") String activaEq);

    // ------------------- LECTURA PARA EL HISTORIAL -------------------
    //
    // Alimentan el refresco del historial. El filtro va por registrado_por
    // (el correo de la sesión) con la sintaxis de PostgREST: "eq.correo@dominio".
    // RLS deja leer todo a cualquier autenticado, así que el recorte por usuario
    // es una decisión de la pantalla, no una barrera de seguridad.

    @GET("registro_biometria")
    Call<List<BiometriaDto>> listarBiometria(@Query("registrado_por") String correoEq,
                                             @Query("order") String orden);

    @GET("registro_agua")
    Call<List<AguaDto>> listarAgua(@Query("registrado_por") String correoEq,
                                   @Query("order") String orden);

    @GET("ensayo_laboratorio")
    Call<List<LaboratorioDto>> listarLaboratorio(@Query("registrado_por") String correoEq,
                                                 @Query("order") String orden);

    // ------------------- SUBIDA DE REGISTROS -------------------

    @POST("registro_biometria")
    Call<Void> subirBiometria(@Header("Prefer") String prefer, @Body List<BiometriaDto> lote);

    @POST("registro_agua")
    Call<Void> subirAgua(@Header("Prefer") String prefer, @Body List<AguaDto> lote);

    @POST("ensayo_laboratorio")
    Call<Void> subirLaboratorio(@Header("Prefer") String prefer, @Body List<LaboratorioDto> lote);
}
