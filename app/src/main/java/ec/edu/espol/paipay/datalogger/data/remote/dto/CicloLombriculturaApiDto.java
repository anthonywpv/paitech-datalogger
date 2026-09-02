package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CicloLombriculturaApiDto {
    public String id;
    public String cama;
    @SerializedName("cama_codigo") public String camaCodigo;
    public int numero;
    public String estado;
    @SerializedName("iniciado_en") public String iniciadoEn;
    @SerializedName("conteo_inicial") public int conteoInicial;
    @SerializedName("observaciones_apertura") public String observacionesApertura;
    @SerializedName("autor_apertura") public JornadaApiDto.AutorDto autorApertura;
    @SerializedName("dispositivo_id") public String dispositivoId;
    @SerializedName("cerrado_en") public String cerradoEn;
    @SerializedName("conteo_final") public Integer conteoFinal;
    @SerializedName("observaciones_cierre") public String observacionesCierre;
    public Integer version;
}
