package ec.edu.espol.paipay.datalogger.data.repo;

/** Información inmutable que la interfaz necesita para una decisión explícita. */
public class DetalleConflicto {
    public final String tipo;
    public final String entidadUuid;
    public final String comparacion;
    public final int versionRemota;
    public final boolean puedeReaplicar;
    public final String estadoRemoto;

    public DetalleConflicto(String tipo, String entidadUuid, String comparacion,
                            int versionRemota, boolean puedeReaplicar,
                            String estadoRemoto) {
        this.tipo = tipo;
        this.entidadUuid = entidadUuid;
        this.comparacion = comparacion;
        this.versionRemota = versionRemota;
        this.puedeReaplicar = puedeReaplicar;
        this.estadoRemoto = estadoRemoto;
    }
}
