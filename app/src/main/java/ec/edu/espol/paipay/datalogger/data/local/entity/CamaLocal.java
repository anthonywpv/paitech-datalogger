package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "cama_local", indices = {
        @Index(value = "codigo", unique = true), @Index("activa")
})
public class CamaLocal {
    @PrimaryKey @NonNull public String uuid = "";
    @NonNull public String codigo = "";
    @NonNull public String nombre = "";
    public String descripcion;
    public Double areaM2;
    public boolean activa;
    public String cicloActivoUuid;
    public Integer cicloActivoNumero;

    @NonNull @Override public String toString() { return codigo + " · " + nombre; }
}
