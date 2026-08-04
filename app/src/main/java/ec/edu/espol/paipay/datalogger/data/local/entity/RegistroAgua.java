package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Medición de calidad de agua de una piscina. Alimenta directamente
 * el "Semáforo de Alertas" (temperatura y oxígeno disuelto).
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

    /** Temperatura del agua en grados Celsius. */
    @ColumnInfo(name = "temperatura_c")
    public double temperaturaC;

    /** Oxígeno disuelto en mg/L. */
    @ColumnInfo(name = "oxigeno_mg_l")
    public double oxigenoMgL;

    /** pH del agua (escala 0-14). Opcional. */
    @ColumnInfo(name = "ph")
    public Double ph;

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
