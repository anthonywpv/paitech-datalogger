package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Instantanea de la version remota que produjo un conflicto optimista.
 *
 * La version elegida por el acuicultor permanece en la tabla de la entidad. De
 * esta forma nunca se reemplaza el cambio local mientras la persona compara y
 * decide como resolverlo.
 */
@Entity(tableName = "conflicto_local", indices = {
        @Index("entidadUuid"), @Index("tipo")
})
public class ConflictoLocal {
    public static final String JORNADA = "JORNADA";
    public static final String MOVIMIENTO = "MOVIMIENTO";

    @PrimaryKey @NonNull public String clave = "";
    @NonNull public String tipo = "";
    @NonNull public String entidadUuid = "";
    @NonNull public String operacionLocal = JornadaLocal.PENDIENTE_EDITAR;
    public int versionRemota;
    public String remotoJson;
    public long detectadoEn;

    public static String clave(String tipo, String entidadUuid) {
        return tipo + ":" + entidadUuid;
    }
}
