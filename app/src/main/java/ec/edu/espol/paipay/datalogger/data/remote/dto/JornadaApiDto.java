package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class JornadaApiDto {
    public String id;
    public String piscina;
    public String ciclo;
    @SerializedName("piscina_codigo") public String piscinaCodigo;
    public String especie;
    public AutorDto autor;
    @SerializedName("capturada_en") public String capturadaEn;
    @SerializedName("poblacion_estimada") public Integer poblacionEstimada;
    public String observaciones;
    @SerializedName("dispositivo_id") public String dispositivoId;
    public String estado;
    public Integer version;
    @SerializedName("motivo_correccion") public String motivoCorreccion;
    public AguaDto agua;
    public List<PezDto> peces = new ArrayList<>();

    public static class AguaDto {
        public String ph;
        public String nitrato;
        public String nitrito;
        @SerializedName("amoniaco_total") public String amoniacoTotal;
    }

    public static class PezDto {
        public String id;
        public int orden;
        @SerializedName("peso_gramos") public String pesoGramos;
        @SerializedName("talla_centimetros") public String tallaCentimetros;
    }

    public static class AutorDto {
        public long id;
        public String nombre;
        public String correo;
    }
}
