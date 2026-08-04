package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Resultado de un ensayo de laboratorio (agua, suelo, humus o abono).
 *
 * Es la pieza de "Consolidación de Fuentes": permite que un dato analítico
 * externo se una, por fecha y piscina, con los datos tomados in situ.
 *
 * Fuente del dato: LABORATORIO.
 */
@Entity(tableName = "ensayo_laboratorio",
        indices = {@Index(value = "uuid", unique = true), @Index("sincronizado")})
public class EnsayoLaboratorio {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "uuid")
    public String uuid = "";

    /** Fecha en que se tomó la muestra enviada al laboratorio. */
    @NonNull
    @ColumnInfo(name = "fecha_muestreo")
    public String fechaMuestreo = "";

    /** Piscina o lecho de lombricultura de origen. */
    @NonNull
    @ColumnInfo(name = "piscina")
    public String piscina = "";

    /** Código con el que el laboratorio identifica la muestra. */
    @ColumnInfo(name = "codigo_muestra")
    public String codigoMuestra;

    /** Agua de piscina, suelo, humus de lombriz, abono orgánico, alimento. */
    @NonNull
    @ColumnInfo(name = "tipo_muestra")
    public String tipoMuestra = "";

    /** Parámetro analizado: nitrógeno total, fósforo, pH, etc. */
    @NonNull
    @ColumnInfo(name = "parametro")
    public String parametro = "";

    @ColumnInfo(name = "valor")
    public double valor;

    @ColumnInfo(name = "unidad")
    public String unidad;

    @ColumnInfo(name = "laboratorio")
    public String laboratorio;

    @ColumnInfo(name = "observacion")
    public String observacion;

    @ColumnInfo(name = "registrado_por")
    public String registradoPor;

    @ColumnInfo(name = "creado_en")
    public long creadoEn;

    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado;

    @ColumnInfo(name = "sincronizado_en")
    public Long sincronizadoEn;
}
