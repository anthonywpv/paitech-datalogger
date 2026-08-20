package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CierreCicloDto {
    public int version;
    @SerializedName("cerrado_en") public String cerradoEn;
    @SerializedName("destino_cierre") public String destinoCierre;
    @SerializedName("poblacion_final") public int poblacionFinal;
    @SerializedName("peso_total_cosechado_kg") public String pesoTotalCosechadoKg;
    @SerializedName("observaciones_cierre") public String observacionesCierre;
    @SerializedName("piscina_destino_cierre") public String piscinaDestinoCierre;
}
