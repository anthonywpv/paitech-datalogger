package ec.edu.espol.paipay.datalogger.sync;

public class ResumenPendientes {
    public final int jornadas;
    public final int movimientos;
    public final int anulaciones;

    public ResumenPendientes(int jornadas, int movimientos, int anulaciones) {
        this.jornadas = jornadas;
        this.movimientos = movimientos;
        this.anulaciones = anulaciones;
    }
    public int total() { return jornadas + movimientos + anulaciones; }
    public boolean hayPendientes() { return total() > 0; }
    public String detalle() {
        StringBuilder texto = new StringBuilder();
        if (jornadas > 0) texto.append("  •  ").append(jornadas).append(jornadas == 1 ? " jornada nueva o corregida\n" : " jornadas nuevas o corregidas\n");
        if (movimientos > 0) texto.append("  •  ").append(movimientos).append(movimientos == 1 ? " movimiento poblacional\n" : " movimientos poblacionales\n");
        if (anulaciones > 0) texto.append("  •  ").append(anulaciones).append(anulaciones == 1 ? " anulación\n" : " anulaciones\n");
        return texto.toString().trim();
    }
}
