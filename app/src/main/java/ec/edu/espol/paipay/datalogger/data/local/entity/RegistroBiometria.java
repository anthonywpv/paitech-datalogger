package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/**
 * Registro de biometría de Vieja Azul (Andinoacara rivulatus) tomado en campo.
 * Corresponde a la actividad "Monitoreo y biometría de peces: medir talla y peso".
 *
 * UNA FILA = UN PEZ. Antes la fila guardaba un peso y una talla junto a un
 * "número de peces muestreados", lo que obligaba a promediar en campo y hacía
 * imposible conocer la medida individual; sin medidas individuales no hay
 * dispersión, y sin dispersión el factor de condición promedio engaña.
 *
 * Los peces medidos el mismo día forman un MUESTREO, identificado por
 * FechaUtil.codigoMuestreo(fechaMuestreo) — "M-04082026". Ese código se deriva,
 * no se guarda, para que no pueda quedar desfasado si se corrige la fecha.
 *
 * Fuente del dato: IN SITU (productor o estudiante en la piscina).
 */
@Entity(tableName = "registro_biometria",
        indices = {@Index(value = "uuid", unique = true), @Index("sincronizado"),
                   @Index({"fecha_muestreo", "piscina"})})
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

    /** Peso de ESTE pez en gramos. */
    @ColumnInfo(name = "peso_g")
    public double pesoGramos;

    /** Talla (longitud total) de ESTE pez en centímetros. */
    @ColumnInfo(name = "talla_cm")
    public double tallaCm;

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

    /** Muestreo al que pertenece este pez: "M-04082026". Derivado, no almacenado. */
    public String codigoMuestreo() {
        return FechaUtil.codigoMuestreo(fechaMuestreo);
    }
}
