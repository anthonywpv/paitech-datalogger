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

    public static LaboratorioDto desde(EnsayoLaboratorio e) {
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
        return d;
    }
}
