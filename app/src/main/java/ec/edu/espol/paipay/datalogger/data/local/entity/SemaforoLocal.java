package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "semaforo_local")
public class SemaforoLocal {
    @PrimaryKey @NonNull public String piscinaUuid = "";
    @NonNull public String piscinaCodigo = "";
    @NonNull public String piscinaNombre = "";
    public String jornadaUuid;
    public String capturadaEn;
    public String autorNombre;
    public Double ph;
    public Double nitrato;
    public Double nitrito;
    public Double amoniacoTotal;
    public String estado;
    public String resumen;
    public long actualizadoEn;
}
