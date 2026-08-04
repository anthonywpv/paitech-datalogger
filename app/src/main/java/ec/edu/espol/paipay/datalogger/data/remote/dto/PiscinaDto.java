package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Fila de la tabla public.piscina en Neon (catálogo remoto). */
public class PiscinaDto {

    @SerializedName("codigo")  public String codigo;
    @SerializedName("nombre")  public String nombre;
    @SerializedName("area_m2") public Double areaM2;
    @SerializedName("activa")  public Boolean activa;
}
