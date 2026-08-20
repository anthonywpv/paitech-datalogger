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
    @SerializedName("ciclo_activo") public CicloApiDto cicloActivo;
    public RecordatoriosDto recordatorios;

    public static class RecordatoriosDto {
        public AvisoDto agua;
        public AvisoDto biometria;
        @SerializedName("ciclo_id") public String cicloId;
    }
    public static class AvisoDto {
        public String estado;
        @SerializedName("vence_en") public String venceEn;
    }

    public static class EspecieDto {
        public long id;
        @SerializedName("nombre_comun") public String nombreComun;
    }
}
