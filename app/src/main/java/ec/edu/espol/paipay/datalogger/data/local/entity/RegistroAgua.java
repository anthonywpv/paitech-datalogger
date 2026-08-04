package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Medición de calidad de agua de una piscina. Alimenta el "Semáforo de Alertas".
 *
 * Los parámetros son los del CICLO DEL NITRÓGENO más el pH. En una piscina el
 * nitrógeno sigue siempre el mismo camino:
 *
 *     los peces excretan AMONIO → las bacterias lo oxidan a NITRITO → y este a NITRATO
 *
 * Los dos primeros son tóxicos; el nitrato es el producto final y el más
 * inocuo. Por eso una lectura no se interpreta sola: amonio alto con nitrito
 * bajo es una piscina cuyo filtro biológico aún no arranca, mientras que
 * nitrito alto es el ciclo a medio hacer.
 *
 * El pH va aparte pero manda sobre el amonio: decide qué fracción está en su
 * forma tóxica (ver EvaluadorSemaforo.amoniacoLibre).
 *
 * Fuente del dato: IN SITU (lectura manual o del módulo de sensores de FIEC).
 */
@Entity(tableName = "registro_agua",
        indices = {@Index(value = "uuid", unique = true), @Index("sincronizado"), @Index("piscina")})
public class RegistroAgua {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "uuid")
    public String uuid = "";

    @NonNull
    @ColumnInfo(name = "fecha_muestreo")
    public String fechaMuestreo = "";

    @NonNull
    @ColumnInfo(name = "piscina")
    public String piscina = "";

    /** pH del agua (escala 0-14). Obligatorio: decide la toxicidad del amonio. */
    @ColumnInfo(name = "ph")
    public double ph;

    /** Nitrato (NO3-) en mg/L. Producto final del ciclo, el menos tóxico. */
    @ColumnInfo(name = "nitrato_mg_l")
    public double nitratoMgL;

    /** Nitrito (NO2-) en mg/L. Muy tóxico: impide a la sangre transportar oxígeno. */
    @ColumnInfo(name = "nitrito_mg_l")
    public double nitritoMgL;

    /** Amonio total (NH4+ + NH3) en mg/L, tal como lo mide un kit de campo. */
    @ColumnInfo(name = "amonio_mg_l")
    public double amonioMgL;

    /** Peces estimados en la piscina. Opcional: no se cuenta en cada muestreo. */
    @ColumnInfo(name = "poblacion_estimada")
    public Integer poblacionEstimada;

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
