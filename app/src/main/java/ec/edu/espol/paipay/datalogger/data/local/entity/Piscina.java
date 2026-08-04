package ec.edu.espol.paipay.datalogger.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Catálogo local de piscinas / lechos del recinto Paipayales. */
@Entity(tableName = "piscina")
public class Piscina {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "codigo")
    public String codigo = "";

    @ColumnInfo(name = "nombre")
    public String nombre;

    /** Área en metros cuadrados (referencial). */
    @ColumnInfo(name = "area_m2")
    public Double areaM2;

    @ColumnInfo(name = "activa")
    public boolean activa = true;

    public Piscina() { }

    public Piscina(@NonNull String codigo, String nombre, Double areaM2) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.areaM2 = areaM2;
    }

    /**
     * Texto que ve el productor en la lista desplegable.
     * Empieza siempre por el código para que la app pueda recuperarlo al guardar.
     */
    @NonNull
    @Override
    public String toString() {
        return nombre == null || nombre.isEmpty() ? codigo : codigo + " · " + nombre;
    }
}
