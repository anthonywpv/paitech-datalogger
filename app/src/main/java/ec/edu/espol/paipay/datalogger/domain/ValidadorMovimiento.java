package ec.edu.espol.paipay.datalogger.domain;

import java.util.Arrays;
import java.util.Objects;

/** Reglas puras para que un movimiento afecte la población en una dirección válida. */
public final class ValidadorMovimiento {
    private ValidadorMovimiento() { }

    /** Devuelve {@code null} cuando es válido o un mensaje entendible cuando no lo es. */
    public static String validar(String tipo, int cantidad,
                                 String origenUuid, String destinoUuid,
                                 String especieOrigen, String especieDestino) {
        if (cantidad <= 0) return "La cantidad debe ser mayor que cero.";
        boolean origen = origenUuid != null;
        boolean destino = destinoUuid != null;

        if ("SIEMBRA".equals(tipo)) {
            return "La siembra se registra iniciando un ciclo productivo.";
        }
        if (Arrays.asList("MORTALIDAD", "COSECHA_VENTA", "ESCAPE").contains(tipo)) {
            return origen && !destino ? null : "Este movimiento requiere solo una piscina de origen.";
        }
        if ("TRASLADO".equals(tipo)) {
            if (!origen || !destino) return "El traslado requiere origen y destino.";
            if (origenUuid.equals(destinoUuid)) return "El origen y el destino deben ser distintos.";
            if (!Objects.equals(especieOrigen, especieDestino)) {
                return "El traslado requiere piscinas de la misma especie.";
            }
            return null;
        }
        if ("AJUSTE".equals(tipo)) {
            return origen != destino ? null : "El ajuste requiere exactamente una piscina afectada.";
        }
        return "El tipo de movimiento no es válido.";
    }
}
