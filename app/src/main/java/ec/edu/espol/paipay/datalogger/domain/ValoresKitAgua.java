package ec.edu.espol.paipay.datalogger.domain;

/** Valores discretos impresos en el API Freshwater Master Test Kit de Paipayales. */
public final class ValoresKitAgua {
    public static final String[] PH = {
            "6.0", "6.4", "6.6", "6.8", "7.0", "7.2", "7.4",
            "7.6", "7.8", "8.0", "8.2", "8.4", "8.8"
    };
    public static final String[] NITRATO = {"0", "5", "10", "20", "40", "80", "160"};
    public static final String[] NITRITO = {"0", "0.25", "0.50", "1.0", "2.0", "5.0"};
    public static final String[] AMONIACO_TOTAL = {
            "0", "0.25", "0.50", "1.0", "2.0", "4.0", "8.0"
    };

    private ValoresKitAgua() { }

    public static boolean contiene(Double valor, String[] permitidos) {
        if (valor == null) return false;
        for (String permitido : permitidos) {
            if (Double.compare(valor, Double.parseDouble(permitido)) == 0) return true;
        }
        return false;
    }
}
