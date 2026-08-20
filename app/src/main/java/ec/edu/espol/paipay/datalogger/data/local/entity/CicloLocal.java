package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "ciclo_local", indices = {
        @Index("piscinaUuid"), @Index("estado"), @Index("estadoLocal"),
        @Index(value = {"piscinaUuid", "numero"}, unique = true)
})
public class CicloLocal {
    public static final String ACTIVO = "ACTIVO";
    public static final String CERRADO = "CERRADO";
    public static final String PENDIENTE_CREAR = "PENDIENTE_CREAR";
    public static final String PENDIENTE_CERRAR = "PENDIENTE_CERRAR";
    public static final String PENDIENTE_CREAR_Y_CERRAR = "PENDIENTE_CREAR_Y_CERRAR";
    public static final String SINCRONIZADO = "SINCRONIZADO";
    public static final String CONFLICTO = "CONFLICTO";

    @PrimaryKey @NonNull public String uuid = "";
    @NonNull public String piscinaUuid = "";
    @NonNull public String piscinaCodigo = "";
    public String especieNombre;
    public int numero;
    @NonNull public String estado = ACTIVO;
    @NonNull public String iniciadoEn = "";
    public int poblacionInicial;
    public Integer duracionEstimadaMeses;
    public String observacionesApertura;
    @NonNull public String autorCorreo = "";

    public String cerradoEn;
    public String destinoCierre;
    public Integer poblacionFinal;
    public Double pesoTotalCosechadoKg;
    public String observacionesCierre;
    public String piscinaDestinoCierreUuid;

    public Integer prediccionPoblacionFinal;
    public Integer prediccionMin;
    public Integer prediccionMax;
    public Double prediccionTasa;
    public int prediccionCiclosUsados;
    public String prediccionConfianza;
    public String prediccionMetodoVersion;
    public String prediccionCalculadaEn;
    public String prediccionDatosHasta;
    public String prediccionOrigen;

    @NonNull public String estadoLocal = PENDIENTE_CREAR;
    public int versionServidor;
    public String errorSincronizacion;
    public long creadaEn;
    public long modificadaEn;

    public boolean pendiente() {
        return PENDIENTE_CREAR.equals(estadoLocal)
                || PENDIENTE_CERRAR.equals(estadoLocal)
                || PENDIENTE_CREAR_Y_CERRAR.equals(estadoLocal);
    }
}
