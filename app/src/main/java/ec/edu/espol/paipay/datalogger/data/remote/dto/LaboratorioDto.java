package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import ec.edu.espol.paipay.datalogger.util.FechaUtil;

import ec.edu.espol.paipay.datalogger.data.local.entity.EnsayoLaboratorio;

/** Cuerpo JSON enviado a la tabla public.ensayo_laboratorio de Neon. */
public class LaboratorioDto {

    @SerializedName("uuid")           public String uuid;
    @SerializedName("fecha_muestreo") public String fechaMuestreo;
    @SerializedName("piscina")        public String piscina;
    @SerializedName("codigo_muestra") public String codigoMuestra;
    @SerializedName("tipo_muestra")   public String tipoMuestra;
    @SerializedName("parametro")      public String parametro;
    @SerializedName("valor")          public double valor;
    @SerializedName("unidad")         public String unidad;
    @SerializedName("laboratorio")    public String laboratorio;
    @SerializedName("observacion")    public String observacion;
    @SerializedName("registrado_por") public String registradoPor;
    @SerializedName("creado_en")      public String creadoEn;
    @SerializedName("sincronizado_en")public String sincronizadoEn;

    public static LaboratorioDto desde(EnsayoLaboratorio e, long momentoSubida) {
        LaboratorioDto d = new LaboratorioDto();
        d.uuid = e.uuid;
        d.fechaMuestreo = e.fechaMuestreo;
        d.piscina = e.piscina;
        d.codigoMuestra = e.codigoMuestra;
        d.tipoMuestra = e.tipoMuestra;
        d.parametro = e.parametro;
        d.valor = e.valor;
        d.unidad = e.unidad;
        d.laboratorio = e.laboratorio;
        d.observacion = e.observacion;
        d.registradoPor = e.registradoPor;
        d.creadoEn = FechaUtil.isoUtc(e.creadoEn);
        d.sincronizadoEn = FechaUtil.isoUtc(momentoSubida);
        return d;
    }

    /** Camino inverso: lo que devuelve Neon al refrescar el historial. */
    public EnsayoLaboratorio aEntidad() {
        EnsayoLaboratorio e = new EnsayoLaboratorio();
        e.uuid = uuid == null ? "" : uuid;
        e.fechaMuestreo = fechaMuestreo == null ? "" : fechaMuestreo;
        e.piscina = piscina == null ? "" : piscina;
        e.codigoMuestra = codigoMuestra;
        e.tipoMuestra = tipoMuestra == null ? "" : tipoMuestra;
        e.parametro = parametro == null ? "" : parametro;
        e.valor = valor;
        e.unidad = unidad;
        e.laboratorio = laboratorio;
        e.observacion = observacion;
        e.registradoPor = registradoPor;
        e.creadoEn = FechaUtil.millisDesdeIsoUtc(creadoEn, System.currentTimeMillis());
        e.sincronizado = true;
        e.sincronizadoEn = FechaUtil.millisDesdeIsoUtc(sincronizadoEn, e.creadoEn);
        return e;
    }
}
