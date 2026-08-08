package ec.edu.espol.paipay.datalogger.ui.historial;

public class ItemHistorial {
    public enum Tipo { JORNADA, MOVIMIENTO, ENCABEZADO }
    public final Tipo tipo;
    public final String uuid;
    public final String titulo;
    public final String detalle;
    public final String fechaMuestreo;
    public final long creadoEn;
    public final boolean sincronizado;
    public final String estadoLocal;

    public ItemHistorial(Tipo tipo, String uuid, String titulo, String detalle,
                         String fechaMuestreo, long creadoEn, boolean sincronizado,
                         String estadoLocal) {
        this.tipo = tipo;
        this.uuid = uuid;
        this.titulo = titulo;
        this.detalle = detalle;
        this.fechaMuestreo = fechaMuestreo;
        this.creadoEn = creadoEn;
        this.sincronizado = sincronizado;
        this.estadoLocal = estadoLocal;
    }

    public boolean esEncabezado() { return tipo == Tipo.ENCABEZADO; }
}
