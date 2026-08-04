package ec.edu.espol.paipay.datalogger.sync;

/**
 * Conteo de registros pendientes por subir.
 *
 * Es exactamente lo que se le muestra al productor en el diálogo de
 * confirmación antes de sincronizar, para que sepa qué está por enviar.
 */
public class ResumenPendientes {

    public final int biometrias;
    public final int aguas;
    public final int ensayos;

    public ResumenPendientes(int biometrias, int aguas, int ensayos) {
        this.biometrias = biometrias;
        this.aguas = aguas;
        this.ensayos = ensayos;
    }

    public int total() {
        return biometrias + aguas + ensayos;
    }

    public boolean hayPendientes() {
        return total() > 0;
    }

    /** Detalle en viñetas para el cuerpo del diálogo. */
    public String detalle() {
        StringBuilder sb = new StringBuilder();
        if (biometrias > 0) {
            sb.append("  •  ").append(biometrias)
              .append(biometrias == 1 ? " registro de biometría de peces (peso y talla)"
                                      : " registros de biometría de peces (peso y talla)")
              .append('\n');
        }
        if (aguas > 0) {
            sb.append("  •  ").append(aguas)
              .append(aguas == 1 ? " medición de calidad de agua (temperatura, oxígeno, pH)"
                                 : " mediciones de calidad de agua (temperatura, oxígeno, pH)")
              .append('\n');
        }
        if (ensayos > 0) {
            sb.append("  •  ").append(ensayos)
              .append(ensayos == 1 ? " ensayo de laboratorio" : " ensayos de laboratorio")
              .append('\n');
        }
        return sb.toString().trim();
    }
}
