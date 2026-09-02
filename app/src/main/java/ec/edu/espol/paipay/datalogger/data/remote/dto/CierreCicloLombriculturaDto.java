package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CierreCicloLombriculturaDto {
    public int version;
    @SerializedName("cerrado_en") public String cerradoEn;
    @SerializedName("conteo_final") public int conteoFinal;
    @SerializedName("observaciones_cierre") public String observacionesCierre;
}
