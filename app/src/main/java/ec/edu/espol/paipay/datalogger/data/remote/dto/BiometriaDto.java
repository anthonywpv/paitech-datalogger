package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import ec.edu.espol.paipay.datalogger.util.FechaUtil;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;

/** Cuerpo JSON enviado a la tabla public.registro_biometria de Neon. */
public class BiometriaDto {

    @SerializedName("uuid")               public String uuid;
    @SerializedName("fecha_muestreo")     public String fechaMuestreo;
    @SerializedName("piscina")            public String piscina;
    @SerializedName("peso_g")             public double pesoG;
    @SerializedName("talla_cm")           public double tallaCm;
    @SerializedName("cantidad_muestreada")public int cantidadMuestreada;
    @SerializedName("factor_condicion")   public double factorCondicion;
    @SerializedName("observacion")        public String observacion;
    @SerializedName("registrado_por")     public String registradoPor;
    @SerializedName("creado_en")          public String creadoEn;

    public static BiometriaDto desde(RegistroBiometria r) {
        BiometriaDto d = new BiometriaDto();
        d.uuid = r.uuid;
        d.fechaMuestreo = r.fechaMuestreo;
        d.piscina = r.piscina;
        d.pesoG = r.pesoGramos;
        d.tallaCm = r.tallaCm;
        d.cantidadMuestreada = r.cantidadMuestreada;
        d.factorCondicion = Math.round(r.factorCondicion() * 1000d) / 1000d;
        d.observacion = r.observacion;
        d.registradoPor = r.registradoPor;
        d.creadoEn = FechaUtil.isoUtc(r.creadoEn);
        return d;
    }
}
