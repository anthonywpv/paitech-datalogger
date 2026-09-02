package ec.edu.espol.paipay.datalogger.data.remote;

import java.util.List;
import java.util.Map;

import ec.edu.espol.paipay.datalogger.data.remote.dto.JornadaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CicloApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CierreCicloDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CamaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CicloLombriculturaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CierreCicloLombriculturaDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.LoginRespuestaDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.MovimientoApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.PiscinaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.SemaforoApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.RegistroLombriculturaApiDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface DjangoApiService {
    @POST("api/v1/auth/login/")
    Call<LoginRespuestaDto> iniciarSesion(@Body Map<String, String> credenciales);

    @POST("api/v1/auth/logout/")
    Call<Void> cerrarSesion(@Header("Authorization") String autorizacion);

    @POST("api/v1/auth/cambiar-clave/")
    Call<Void> cambiarClave(@Body Map<String, String> datos);

    @GET("api/v1/catalogos/piscinas/")
    Call<List<PiscinaApiDto>> piscinas();

    @GET("api/v1/catalogos/camas/")
    Call<List<CamaApiDto>> camas();

    @GET("api/v1/lombricultura/ciclos/")
    Call<List<CicloLombriculturaApiDto>> ciclosLombricultura();

    @GET("api/v1/lombricultura/ciclos/{id}/")
    Call<CicloLombriculturaApiDto> cicloLombricultura(@Path("id") String id);

    @POST("api/v1/lombricultura/ciclos/")
    Call<CicloLombriculturaApiDto> crearCicloLombricultura(
            @Body CicloLombriculturaApiDto ciclo);

    @POST("api/v1/lombricultura/ciclos/{id}/cerrar/")
    Call<CicloLombriculturaApiDto> cerrarCicloLombricultura(
            @Path("id") String id, @Body CierreCicloLombriculturaDto cierre);

    @GET("api/v1/lombricultura/registros/")
    Call<List<RegistroLombriculturaApiDto>> registrosLombricultura();

    @GET("api/v1/lombricultura/registros/{id}/")
    Call<RegistroLombriculturaApiDto> registroLombricultura(@Path("id") String id);

    @POST("api/v1/lombricultura/registros/")
    Call<RegistroLombriculturaApiDto> crearRegistroLombricultura(
            @Body RegistroLombriculturaApiDto registro);

    @PUT("api/v1/lombricultura/registros/{id}/")
    Call<RegistroLombriculturaApiDto> editarRegistroLombricultura(
            @Path("id") String id, @Body RegistroLombriculturaApiDto registro);

    @POST("api/v1/lombricultura/registros/{id}/anular/")
    Call<RegistroLombriculturaApiDto> anularRegistroLombricultura(
            @Path("id") String id, @Body Map<String, Object> anulacion);

    @GET("api/v1/ciclos/")
    Call<List<CicloApiDto>> ciclos();

    @GET("api/v1/ciclos/{id}/")
    Call<CicloApiDto> ciclo(@Path("id") String id);

    @POST("api/v1/ciclos/")
    Call<CicloApiDto> crearCiclo(@Body CicloApiDto ciclo);

    @POST("api/v1/ciclos/{id}/cerrar/")
    Call<CicloApiDto> cerrarCiclo(@Path("id") String id, @Body CierreCicloDto cierre);

    @GET("api/v1/jornadas/")
    Call<List<JornadaApiDto>> jornadas();

    @GET("api/v1/jornadas/{id}/")
    Call<JornadaApiDto> jornada(@Path("id") String id);

    @POST("api/v1/jornadas/")
    Call<JornadaApiDto> crearJornada(@Body JornadaApiDto jornada);

    @PUT("api/v1/jornadas/{id}/")
    Call<JornadaApiDto> editarJornada(@Path("id") String id, @Body JornadaApiDto jornada);

    @POST("api/v1/jornadas/{id}/anular/")
    Call<JornadaApiDto> anularJornada(@Path("id") String id, @Body Map<String, Object> anulacion);

    @GET("api/v1/semaforos/")
    Call<List<SemaforoApiDto>> semaforos();

    @GET("api/v1/movimientos/")
    Call<List<MovimientoApiDto>> movimientos();

    @GET("api/v1/movimientos/{id}/")
    Call<MovimientoApiDto> movimiento(@Path("id") String id);

    @POST("api/v1/movimientos/")
    Call<MovimientoApiDto> crearMovimiento(@Body MovimientoApiDto movimiento);

    @PUT("api/v1/movimientos/{id}/")
    Call<MovimientoApiDto> editarMovimiento(@Path("id") String id, @Body MovimientoApiDto movimiento);

    @POST("api/v1/movimientos/{id}/anular/")
    Call<MovimientoApiDto> anularMovimiento(@Path("id") String id, @Body Map<String, Object> anulacion);
}
