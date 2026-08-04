package ec.edu.espol.paipay.datalogger.ui.historial;

/** Fila unificada del historial: sirve para los tres tipos de registro. */
public class ItemHistorial {

    public enum Tipo { BIOMETRIA, AGUA, LABORATORIO }

    public final Tipo tipo;
    public final String titulo;
    public final String detalle;
    public final String fechaMuestreo;
    public final long creadoEn;
    public final boolean sincronizado;

    public ItemHistorial(Tipo tipo, String titulo, String detalle,
                         String fechaMuestreo, long creadoEn, boolean sincronizado) {
        this.tipo = tipo;
        this.titulo = titulo;
        this.detalle = detalle;
        this.fechaMuestreo = fechaMuestreo;
        this.creadoEn = creadoEn;
        this.sincronizado = sincronizado;
    }
}
