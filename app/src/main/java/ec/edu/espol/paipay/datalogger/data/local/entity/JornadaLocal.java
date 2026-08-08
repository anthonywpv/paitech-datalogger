package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "jornada_local", indices = {
        @Index("autorCorreo"), @Index("piscinaUuid"), @Index("capturadaEn"),
        @Index("estadoLocal")
})
public class JornadaLocal {
    public static final String BORRADOR = "BORRADOR";
    public static final String PENDIENTE_CREAR = "PENDIENTE_CREAR";
    public static final String PENDIENTE_EDITAR = "PENDIENTE_EDITAR";
    public static final String PENDIENTE_ANULAR = "PENDIENTE_ANULAR";
    public static final String SINCRONIZADO = "SINCRONIZADO";
    public static final String CONFLICTO = "CONFLICTO";
    public static final String ANULADO = "ANULADO";
    public static final String ANULADO_LOCAL = "ANULADO_LOCAL";

    @PrimaryKey @NonNull public String uuid = "";
    @NonNull public String piscinaUuid = "";
    @NonNull public String piscinaCodigo = "";
    public String especieNombre;
    @NonNull public String autorCorreo = "";
    @NonNull public String capturadaEn = "";
    public Integer poblacionEstimada;
    public String observaciones;
    public boolean incluyeAgua;
    public Double ph;
    public Double nitrato;
    public Double nitrito;
    public Double amonio;
    @NonNull public String estadoLocal = BORRADOR;
    public int versionServidor;
    public String motivoCambio;
    public String errorSincronizacion;
    public long creadaEn;
    public long modificadaEn;

    public boolean estaCompleta() {
        return poblacionEstimada != null && (incluyeAgua || tieneBiometriaPendiente);
    }

    // Campo transitorio que usa la capa de dominio al validar junto a sus peces.
    @androidx.room.Ignore public boolean tieneBiometriaPendiente;

    public boolean estaPendiente() {
        return PENDIENTE_CREAR.equals(estadoLocal)
                || PENDIENTE_EDITAR.equals(estadoLocal)
                || PENDIENTE_ANULAR.equals(estadoLocal);
    }
}
