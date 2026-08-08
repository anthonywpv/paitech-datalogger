package ec.edu.espol.paipay.datalogger.data.remote;

import java.util.List;
import java.util.Map;

import ec.edu.espol.paipay.datalogger.data.remote.dto.JornadaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.LoginRespuestaDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.MovimientoApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.PiscinaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.SemaforoApiDto;
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

    @GET("api/v1/catalogos/piscinas/")
    Call<List<PiscinaApiDto>> piscinas();

    @GET("api/v1/jornadas/")
    Call<List<JornadaApiDto>> jornadas();

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

    @POST("api/v1/movimientos/")
    Call<MovimientoApiDto> crearMovimiento(@Body MovimientoApiDto movimiento);

    @PUT("api/v1/movimientos/{id}/")
    Call<MovimientoApiDto> editarMovimiento(@Path("id") String id, @Body MovimientoApiDto movimiento);

    @POST("api/v1/movimientos/{id}/anular/")
    Call<MovimientoApiDto> anularMovimiento(@Path("id") String id, @Body Map<String, Object> anulacion);
}
