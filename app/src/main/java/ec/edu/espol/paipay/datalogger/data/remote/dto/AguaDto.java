package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import ec.edu.espol.paipay.datalogger.util.FechaUtil;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;
import ec.edu.espol.paipay.datalogger.domain.EvaluadorSemaforo;

/** Cuerpo JSON enviado a la tabla public.registro_agua de Neon. */
public class AguaDto {

    @SerializedName("uuid")           public String uuid;
    @SerializedName("fecha_muestreo") public String fechaMuestreo;
    @SerializedName("piscina")        public String piscina;
    @SerializedName("temperatura_c")  public double temperaturaC;
    @SerializedName("oxigeno_mg_l")   public double oxigenoMgL;
    @SerializedName("ph")             public Double ph;
    @SerializedName("estado_alerta")  public String estadoAlerta;
    @SerializedName("observacion")    public String observacion;
    @SerializedName("registrado_por") public String registradoPor;
    @SerializedName("creado_en")      public String creadoEn;
    @SerializedName("sincronizado_en")public String sincronizadoEn;

    public static AguaDto desde(RegistroAgua r, long momentoSubida) {
        AguaDto d = new AguaDto();
        d.uuid = r.uuid;
        d.fechaMuestreo = r.fechaMuestreo;
        d.piscina = r.piscina;
        d.temperaturaC = r.temperaturaC;
        d.oxigenoMgL = r.oxigenoMgL;
        d.ph = r.ph;
        // El estado del semáforo se calcula en el dispositivo y viaja junto al
        // dato, para que el análisis en la nube no tenga que recalcularlo.
        d.estadoAlerta = EvaluadorSemaforo.evaluar(r).getEstadoGlobal().name();
        d.observacion = r.observacion;
        d.registradoPor = r.registradoPor;
        d.creadoEn = FechaUtil.isoUtc(r.creadoEn);
        d.sincronizadoEn = FechaUtil.isoUtc(momentoSubida);
        return d;
    }

    /** Camino inverso: lo que devuelve Neon al refrescar el historial. */
    public RegistroAgua aEntidad() {
        RegistroAgua r = new RegistroAgua();
        r.uuid = uuid == null ? "" : uuid;
        r.fechaMuestreo = fechaMuestreo == null ? "" : fechaMuestreo;
        r.piscina = piscina == null ? "" : piscina;
        r.temperaturaC = temperaturaC;
        r.oxigenoMgL = oxigenoMgL;
        r.ph = ph;
        r.observacion = observacion;
        r.registradoPor = registradoPor;
        r.creadoEn = FechaUtil.millisDesdeIsoUtc(creadoEn, System.currentTimeMillis());
        r.sincronizado = true;
        r.sincronizadoEn = FechaUtil.millisDesdeIsoUtc(sincronizadoEn, r.creadoEn);
        return r;
    }
}
