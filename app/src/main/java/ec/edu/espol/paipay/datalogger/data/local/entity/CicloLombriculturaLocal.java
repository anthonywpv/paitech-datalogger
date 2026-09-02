package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "ciclo_lombricultura_local", indices = {
        @Index("camaUuid"), @Index("estado"), @Index("estadoLocal"),
        @Index(value = {"camaUuid", "numero"}, unique = true)
})
public class CicloLombriculturaLocal {
    public static final String ACTIVO = "ACTIVO";
    public static final String CERRADO = "CERRADO";
    public static final String PENDIENTE_CREAR = "PENDIENTE_CREAR";
    public static final String PENDIENTE_CERRAR = "PENDIENTE_CERRAR";
    public static final String PENDIENTE_CREAR_Y_CERRAR = "PENDIENTE_CREAR_Y_CERRAR";
    public static final String SINCRONIZADO = "SINCRONIZADO";
    public static final String CONFLICTO = "CONFLICTO";

    @PrimaryKey @NonNull public String uuid = "";
    @NonNull public String camaUuid = "";
    @NonNull public String camaCodigo = "";
    public int numero;
    @NonNull public String estado = ACTIVO;
    @NonNull public String iniciadoEn = "";
    public int conteoInicial;
    public String observacionesApertura;
    @NonNull public String autorCorreo = "";
    public String cerradoEn;
    public Integer conteoFinal;
    public String observacionesCierre;
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
