package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RegistroLombriculturaApiDto {
    public String id;
    public String cama;
    public String ciclo;
    @SerializedName("cama_codigo") public String camaCodigo;
    public JornadaApiDto.AutorDto autor;
    @SerializedName("capturada_en") public String capturadaEn;
    @SerializedName("ph_suelo") public String phSuelo;
    @SerializedName("conteo_lombrices") public int conteoLombrices;
    public String observaciones;
    @SerializedName("dispositivo_id") public String dispositivoId;
    public String estado;
    public Integer version;
    @SerializedName("motivo_correccion") public String motivoCorreccion;
}
