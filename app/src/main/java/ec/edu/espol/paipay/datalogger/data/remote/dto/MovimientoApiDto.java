package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class MovimientoApiDto {
    public String id;
    public String tipo;
    public int cantidad;
    @SerializedName("piscina_origen") public String piscinaOrigen;
    @SerializedName("piscina_destino") public String piscinaDestino;
    @SerializedName("ciclo_origen") public String cicloOrigen;
    @SerializedName("ciclo_destino") public String cicloDestino;
    @SerializedName("ocurrido_en") public String ocurridoEn;
    public String observaciones;
    public JornadaApiDto.AutorDto autor;
    public String estado;
    public Integer version;
    @SerializedName("motivo_correccion") public String motivoCorreccion;
}
