package ec.edu.espol.paipay.datalogger.ui.historial;

import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/**
 * Fila del historial. Sirve para los tres tipos de registro y, además, para los
 * encabezados que separan un muestreo de otro.
 *
 * La lista que consume el adaptador es plana: encabezado, sus registros,
 * siguiente encabezado, sus registros… Se resuelve así y no con una lista
 * anidada porque RecyclerView recicla mejor una sola lista y el productor
 * necesita poder desplazarse de corrido por todo el historial.
 */
public class ItemHistorial {

    public enum Tipo { BIOMETRIA, AGUA, LABORATORIO, ENCABEZADO }

    public final Tipo tipo;
    /** uuid del registro; null en los encabezados. */
    public final String uuid;
    public final String titulo;
    public final String detalle;
    public final String fechaMuestreo;
    public final long creadoEn;
    public final boolean sincronizado;

    public ItemHistorial(Tipo tipo, String uuid, String titulo, String detalle,
                         String fechaMuestreo, long creadoEn, boolean sincronizado) {
        this.tipo = tipo;
        this.uuid = uuid;
        this.titulo = titulo;
        this.detalle = detalle;
        this.fechaMuestreo = fechaMuestreo;
        this.creadoEn = creadoEn;
        this.sincronizado = sincronizado;
    }

    /** Separador de muestreo: "Muestreo M-04082026" con su número de registros. */
    public static ItemHistorial encabezado(String fechaMuestreo, int cuantos) {
        return new ItemHistorial(Tipo.ENCABEZADO, null,
                FechaUtil.codigoMuestreo(fechaMuestreo),
                String.valueOf(cuantos), fechaMuestreo, 0L, false);
    }

    public boolean esEncabezado() {
        return tipo == Tipo.ENCABEZADO;
    }

    /** Muestreo al que pertenece: derivado de la fecha de muestreo. */
    public String codigoMuestreo() {
        return FechaUtil.codigoMuestreo(fechaMuestreo);
    }
}
