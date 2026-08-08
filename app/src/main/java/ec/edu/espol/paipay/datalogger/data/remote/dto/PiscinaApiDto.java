package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class PiscinaApiDto {
    public String id;
    public String codigo;
    public String nombre;
    public String tipo;
    public String descripcion;
    @SerializedName("area_m2") public String areaM2;
    public EspecieDto especie;

    public static class EspecieDto {
        public long id;
        @SerializedName("nombre_comun") public String nombreComun;
    }
}
