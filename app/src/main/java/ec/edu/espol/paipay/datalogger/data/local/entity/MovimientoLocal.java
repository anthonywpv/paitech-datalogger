package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "movimiento_local", indices = {@Index("autorCorreo"), @Index("estadoLocal"), @Index("ocurridoEn")})
public class MovimientoLocal {
    @PrimaryKey @NonNull public String uuid = "";
    @NonNull public String tipo = "";
    public int cantidad;
    public String piscinaOrigenUuid;
    public String piscinaDestinoUuid;
    @NonNull public String autorCorreo = "";
    @NonNull public String ocurridoEn = "";
    public String observaciones;
    @NonNull public String estadoLocal = JornadaLocal.PENDIENTE_CREAR;
    public int versionServidor;
    public String motivoCambio;
    public String errorSincronizacion;
    public long creadaEn;
    public long modificadaEn;
}
