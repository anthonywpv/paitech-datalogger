package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Registro de biometría de Vieja Azul (Andinoacara rivulatus) tomado en campo.
 * Corresponde a la actividad "Monitoreo y biometría de peces: medir talla y peso".
 *
 * Fuente del dato: IN SITU (productor o estudiante en la piscina).
 */
@Entity(tableName = "registro_biometria",
        indices = {@Index(value = "uuid", unique = true), @Index("sincronizado")})
public class RegistroBiometria {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Identificador único generado en el teléfono. Evita duplicados al sincronizar. */
    @NonNull
    @ColumnInfo(name = "uuid")
    public String uuid = "";

    /** Fecha del muestreo en formato yyyy-MM-dd. */
    @NonNull
    @ColumnInfo(name = "fecha_muestreo")
    public String fechaMuestreo = "";

    @NonNull
    @ColumnInfo(name = "piscina")
    public String piscina = "";

    /** Peso promedio del pez en gramos. */
    @ColumnInfo(name = "peso_g")
    public double pesoGramos;

    /** Talla (longitud total) en centímetros. */
    @ColumnInfo(name = "talla_cm")
    public double tallaCm;

    /** Número de peces incluidos en la muestra. */
    @ColumnInfo(name = "cantidad_muestreada")
    public int cantidadMuestreada;

    @ColumnInfo(name = "observacion")
    public String observacion;

    @ColumnInfo(name = "registrado_por")
    public String registradoPor;

    /** Momento exacto en que se guardó en el teléfono (epoch millis). */
    @ColumnInfo(name = "creado_en")
    public long creadoEn;

    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado;

    @ColumnInfo(name = "sincronizado_en")
    public Long sincronizadoEn;

    /**
     * Factor de condición de Fulton: K = 100 * peso(g) / talla(cm)^3.
     * Indicador clásico de bienestar del pez, útil para el informe técnico.
     */
    public double factorCondicion() {
        if (tallaCm <= 0) return 0d;
        return 100d * pesoGramos / Math.pow(tallaCm, 3);
    }
}
