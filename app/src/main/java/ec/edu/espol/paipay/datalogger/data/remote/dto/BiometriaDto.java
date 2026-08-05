package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import ec.edu.espol.paipay.datalogger.util.FechaUtil;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;

/**
 * Cuerpo JSON enviado a la tabla public.registro_biometria de Neon.
 *
 * No incluye codigo_muestreo a propósito: en Postgres esa columna es
 * GENERATED ALWAYS a partir de fecha_muestreo, así que la calcula el servidor y
 * enviarla daría error. Es la forma de garantizar que el muestreo que ve la app
 * y el que ve el análisis sean siempre el mismo.
 */
public class BiometriaDto {

    @SerializedName("uuid")               public String uuid;
    @SerializedName("fecha_muestreo")     public String fechaMuestreo;
    @SerializedName("piscina")            public String piscina;
    @SerializedName("peso_g")             public double pesoG;
    @SerializedName("talla_cm")           public double tallaCm;
    @SerializedName("observacion")        public String observacion;
    @SerializedName("registrado_por")     public String registradoPor;
    @SerializedName("creado_en")          public String creadoEn;
    @SerializedName("sincronizado_en")    public String sincronizadoEn;

    /**
     * @param momentoSubida instante en que se está sincronizando este lote. Se
     *                      pasa desde fuera porque en la entidad todavía es null:
     *                      solo se marca en la base local si el servidor acepta.
     */
    public static BiometriaDto desde(RegistroBiometria r, long momentoSubida) {
        BiometriaDto d = new BiometriaDto();
        d.uuid = r.uuid;
        d.fechaMuestreo = r.fechaMuestreo;
        d.piscina = r.piscina;
        d.pesoG = r.pesoGramos;
        d.tallaCm = r.tallaCm;
        d.observacion = r.observacion;
        d.registradoPor = r.registradoPor;
        d.creadoEn = FechaUtil.isoUtc(r.creadoEn);
        d.sincronizadoEn = FechaUtil.isoUtc(momentoSubida);
        return d;
    }

    /**
     * Camino inverso: lo que devuelve Neon al refrescar el historial.
     * Se marca sincronizado porque, por definición, ya está en el servidor.
     */
    public RegistroBiometria aEntidad() {
        RegistroBiometria r = new RegistroBiometria();
        r.uuid = uuid == null ? "" : uuid;
        r.fechaMuestreo = fechaMuestreo == null ? "" : fechaMuestreo;
        r.piscina = piscina == null ? "" : piscina;
        r.pesoGramos = pesoG;
        r.tallaCm = tallaCm;
        r.observacion = observacion;
        r.registradoPor = registradoPor;
        r.creadoEn = FechaUtil.millisDesdeIsoUtc(creadoEn, System.currentTimeMillis());
        r.sincronizado = true;
        r.sincronizadoEn = FechaUtil.millisDesdeIsoUtc(sincronizadoEn, r.creadoEn);
        return r;
    }
}
