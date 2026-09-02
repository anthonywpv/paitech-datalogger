package ec.edu.espol.paipay.datalogger.sync;

public class ResumenPendientes {
    public final int jornadas;
    public final int movimientos;
    public final int ciclos;
    public final int anulaciones;
    public final int registrosLombricultura;
    public final int ciclosLombricultura;

    public ResumenPendientes(int jornadas, int movimientos, int ciclos, int anulaciones,
                             int registrosLombricultura, int ciclosLombricultura) {
        this.jornadas = jornadas;
        this.movimientos = movimientos;
        this.ciclos = ciclos;
        this.anulaciones = anulaciones;
        this.registrosLombricultura = registrosLombricultura;
        this.ciclosLombricultura = ciclosLombricultura;
    }
    public int total() { return jornadas + movimientos + ciclos + anulaciones
            + registrosLombricultura + ciclosLombricultura; }
    public boolean hayPendientes() { return total() > 0; }
    public String detalle() {
        StringBuilder texto = new StringBuilder();
        if (jornadas > 0) texto.append("  •  ").append(jornadas).append(jornadas == 1 ? " jornada nueva o corregida\n" : " jornadas nuevas o corregidas\n");
        if (movimientos > 0) texto.append("  •  ").append(movimientos).append(movimientos == 1 ? " movimiento poblacional\n" : " movimientos poblacionales\n");
        if (ciclos > 0) texto.append("  •  ").append(ciclos).append(ciclos == 1 ? " operación de ciclo\n" : " operaciones de ciclo\n");
        if (anulaciones > 0) texto.append("  •  ").append(anulaciones).append(anulaciones == 1 ? " anulación\n" : " anulaciones\n");
        if (registrosLombricultura > 0) texto.append("  •  ").append(registrosLombricultura).append(registrosLombricultura == 1 ? " registro de lombricultura\n" : " registros de lombricultura\n");
        if (ciclosLombricultura > 0) texto.append("  •  ").append(ciclosLombricultura).append(ciclosLombricultura == 1 ? " operación de ciclo de lombricultura\n" : " operaciones de ciclos de lombricultura\n");
        return texto.toString().trim();
    }
}
