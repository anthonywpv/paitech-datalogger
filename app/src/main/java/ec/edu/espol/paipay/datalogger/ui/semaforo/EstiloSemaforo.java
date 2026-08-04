package ec.edu.espol.paipay.datalogger.ui.semaforo;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.domain.EstadoAlerta;

/**
 * Traducción del estado de negocio a recursos visuales.
 *
 * Mantener esta correspondencia en un solo lugar evita que el semáforo se
 * pinte de un color en una pantalla y de otro distinto en otra.
 */
public final class EstiloSemaforo {

    private EstiloSemaforo() { }

    @ColorRes
    public static int color(EstadoAlerta estado) {
        switch (estado) {
            case VERDE:    return R.color.semaforo_verde;
            case AMARILLO: return R.color.semaforo_amarillo;
            case ROJO:     return R.color.semaforo_rojo;
            default:       return R.color.semaforo_gris;
        }
    }

    @ColorRes
    public static int fondo(EstadoAlerta estado) {
        switch (estado) {
            case VERDE:    return R.color.semaforo_verde_fondo;
            case AMARILLO: return R.color.semaforo_amarillo_fondo;
            case ROJO:     return R.color.semaforo_rojo_fondo;
            default:       return R.color.semaforo_gris_fondo;
        }
    }

    @DrawableRes
    public static int punto(EstadoAlerta estado) {
        switch (estado) {
            case VERDE:    return R.drawable.punto_verde;
            case AMARILLO: return R.drawable.punto_amarillo;
            case ROJO:     return R.drawable.punto_rojo;
            default:       return R.drawable.punto_gris;
        }
    }

    @StringRes
    public static int etiqueta(EstadoAlerta estado) {
        switch (estado) {
            case VERDE:    return R.string.semaforo_verde_texto;
            case AMARILLO: return R.string.semaforo_amarillo_texto;
            case ROJO:     return R.string.semaforo_rojo_texto;
            default:       return R.string.semaforo_gris_texto;
        }
    }
}
