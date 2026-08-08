package ec.edu.espol.paipay.datalogger.sync;

import android.content.Context;

/** Adaptador de lectura para la pantalla de historial; la fuente visible sigue siendo Room. */
public class RefrescoHistorialRepositorio {
    public enum Estado { OK, SIN_INTERNET, SIN_SESION, ERROR }
    public static class Resultado {
        public final Estado estado;
        public final int traidos;
        Resultado(Estado estado, int traidos) { this.estado = estado; this.traidos = traidos; }
    }
    public interface Callback { void terminado(Resultado resultado); }
    private final SincronizacionRepositorio sync;

    public RefrescoHistorialRepositorio(Context contexto) {
        sync = new SincronizacionRepositorio(contexto);
    }

    public void refrescar(Callback callback) {
        sync.refrescarSoloLectura(new SincronizacionRepositorio.CallbackSincronizacion() {
            @Override public void progreso(String mensaje) { }
            @Override public void terminado(ResultadoSincronizacion resultado) {
                Estado estado;
                switch (resultado.estado) {
                    case EXITO: estado = Estado.OK; break;
                    case SIN_INTERNET: estado = Estado.SIN_INTERNET; break;
                    case SESION_EXPIRADA: estado = Estado.SIN_SESION; break;
                    default: estado = Estado.ERROR;
                }
                callback.terminado(new Resultado(estado, 0));
            }
        });
    }
}
