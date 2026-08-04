package ec.edu.espol.paipay.datalogger.sync;

/** Resultado de un intento de sincronización. */
public class ResultadoSincronizacion {

    public enum Estado { EXITO, PARCIAL, SIN_PENDIENTES, SIN_INTERNET, SESION_EXPIRADA, ERROR }

    public final Estado estado;
    public final int subidos;
    public final int fallidos;
    public final String mensajeTecnico;

    public ResultadoSincronizacion(Estado estado, int subidos, int fallidos, String mensajeTecnico) {
        this.estado = estado;
        this.subidos = subidos;
        this.fallidos = fallidos;
        this.mensajeTecnico = mensajeTecnico;
    }

    public static ResultadoSincronizacion sinInternet() {
        return new ResultadoSincronizacion(Estado.SIN_INTERNET, 0, 0, null);
    }

    public static ResultadoSincronizacion sinPendientes() {
        return new ResultadoSincronizacion(Estado.SIN_PENDIENTES, 0, 0, null);
    }
}
