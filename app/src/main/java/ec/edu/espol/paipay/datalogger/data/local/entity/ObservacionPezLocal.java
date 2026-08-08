package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "observacion_pez_local",
        foreignKeys = @ForeignKey(
                entity = JornadaLocal.class,
                parentColumns = "uuid",
                childColumns = "jornadaUuid",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("jornadaUuid"), @Index(value = {"jornadaUuid", "orden"}, unique = true)}
)
public class ObservacionPezLocal {
    @PrimaryKey @NonNull public String uuid = "";
    @NonNull public String jornadaUuid = "";
    public int orden;
    public double pesoGramos;
    public double tallaCentimetros;
}
