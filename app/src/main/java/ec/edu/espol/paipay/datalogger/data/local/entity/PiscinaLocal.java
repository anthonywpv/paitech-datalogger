package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "piscina_local", indices = {@Index(value = "codigo", unique = true), @Index("activa")})
public class PiscinaLocal {
    @PrimaryKey @NonNull public String uuid = "";
    @NonNull public String codigo = "";
    @NonNull public String nombre = "";
    @NonNull public String tipo = "";
    public String descripcion;
    public String especieNombre;
    public boolean activa;
    public String cicloActivoUuid;
    public Integer cicloActivoNumero;
    public String recordatorioAguaEstado;
    public String recordatorioBiometriaEstado;
    public String recordatoriosActualizadosEn;

    @Override public String toString() {
        return codigo + " · " + nombre;
    }
}
