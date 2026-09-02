package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CamaApiDto {
    public String id;
    public String codigo;
    public String nombre;
    public String descripcion;
    @SerializedName("area_m2") public String areaM2;
    @SerializedName("ciclo_activo") public CicloLombriculturaApiDto cicloActivo;
    @SerializedName("ultimo_registro") public RegistroLombriculturaApiDto ultimoRegistro;
}
