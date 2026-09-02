package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "registro_lombricultura_local", indices = {
        @Index("camaUuid"), @Index("cicloUuid"), @Index("capturadaEn"),
        @Index("estadoLocal"), @Index("autorCorreo")
})
public class RegistroLombriculturaLocal {
    public static final String PENDIENTE_CREAR = "PENDIENTE_CREAR";
    public static final String PENDIENTE_EDITAR = "PENDIENTE_EDITAR";
    public static final String PENDIENTE_ANULAR = "PENDIENTE_ANULAR";
    public static final String SINCRONIZADO = "SINCRONIZADO";
    public static final String CONFLICTO = "CONFLICTO";
    public static final String ANULADO = "ANULADO";

    @PrimaryKey @NonNull public String uuid = "";
    @NonNull public String camaUuid = "";
    @NonNull public String camaCodigo = "";
    @NonNull public String cicloUuid = "";
    @NonNull public String autorCorreo = "";
    @NonNull public String capturadaEn = "";
    public double phSuelo;
    public int conteoLombrices;
    public String observaciones;
    @NonNull public String estadoLocal = PENDIENTE_CREAR;
    public int versionServidor;
    public String motivoCambio;
    public String errorSincronizacion;
    public long creadaEn;
    public long modificadaEn;

    public boolean pendiente() {
        return PENDIENTE_CREAR.equals(estadoLocal)
                || PENDIENTE_EDITAR.equals(estadoLocal)
                || PENDIENTE_ANULAR.equals(estadoLocal);
    }
}
