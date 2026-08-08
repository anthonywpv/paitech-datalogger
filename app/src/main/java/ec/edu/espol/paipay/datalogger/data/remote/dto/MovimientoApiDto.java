package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class MovimientoApiDto {
    public String id;
    public String tipo;
    public int cantidad;
    @SerializedName("piscina_origen") public String piscinaOrigen;
    @SerializedName("piscina_destino") public String piscinaDestino;
    @SerializedName("ocurrido_en") public String ocurridoEn;
    public String observaciones;
    public String estado;
    public int version;
    @SerializedName("motivo_correccion") public String motivoCorreccion;
}
