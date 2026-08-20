package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CicloApiDto {
    public String id;
    public String piscina;
    @SerializedName("piscina_codigo") public String piscinaCodigo;
    public EspecieDto especie;
    public int numero;
    public String estado;
    @SerializedName("iniciado_en") public String iniciadoEn;
    @SerializedName("poblacion_inicial") public int poblacionInicial;
    @SerializedName("duracion_estimada_meses") public Integer duracionEstimadaMeses;
    @SerializedName("observaciones_apertura") public String observacionesApertura;
    @SerializedName("autor_apertura") public JornadaApiDto.AutorDto autorApertura;
    @SerializedName("dispositivo_id") public String dispositivoId;
    @SerializedName("cerrado_en") public String cerradoEn;
    @SerializedName("destino_cierre") public String destinoCierre;
    @SerializedName("poblacion_final") public Integer poblacionFinal;
    @SerializedName("peso_total_cosechado_kg") public String pesoTotalCosechadoKg;
    @SerializedName("observaciones_cierre") public String observacionesCierre;
    @SerializedName("piscina_destino_cierre") public String piscinaDestinoCierre;
    public PrediccionDto prediccion;
    public Integer version;
    @SerializedName("prediccion_cache") public PrediccionCacheDto prediccionCache;

    public static class EspecieDto {
        public long id;
        @SerializedName("nombre_comun") public String nombreComun;
        @SerializedName("nombre_cientifico") public String nombreCientifico;
    }

    public static class PrediccionDto {
        @SerializedName("poblacion_final") public Integer poblacionFinal;
        public Integer minimo;
        public Integer maximo;
        public String tasa;
        @SerializedName("ciclos_usados") public int ciclosUsados;
        public String confianza;
        @SerializedName("metodo_version") public String metodoVersion;
        @SerializedName("calculada_en") public String calculadaEn;
        @SerializedName("datos_hasta") public String datosHasta;
        public String origen;
    }

    public static class PrediccionCacheDto {
        @SerializedName("prediccion_poblacion_final") public int poblacionFinal;
        @SerializedName("prediccion_min") public int minimo;
        @SerializedName("prediccion_max") public int maximo;
        @SerializedName("prediccion_tasa") public String tasa;
        @SerializedName("prediccion_ciclos_usados") public int ciclosUsados;
        @SerializedName("prediccion_confianza") public String confianza;
        @SerializedName("prediccion_metodo_version") public String metodoVersion;
        @SerializedName("prediccion_calculada_en") public String calculadaEn;
        @SerializedName("prediccion_datos_hasta") public String datosHasta;
    }
}
