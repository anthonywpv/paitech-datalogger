package ec.edu.espol.paipay.datalogger.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Formatos de fecha unificados para toda la app (zona horaria de Ecuador). */
public final class FechaUtil {

    public static final Locale EC = new Locale("es", "EC");
    private static final TimeZone GUAYAQUIL = TimeZone.getTimeZone("America/Guayaquil");

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", EC);
    private static final SimpleDateFormat LEGIBLE = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", EC);
    private static final SimpleDateFormat CORTA = new SimpleDateFormat("dd/MM/yyyy", EC);
    private static final SimpleDateFormat CON_HORA = new SimpleDateFormat("dd/MM/yyyy HH:mm", EC);
    private static final SimpleDateFormat DDMMYYYY = new SimpleDateFormat("ddMMyyyy", EC);

    /** Marca de tiempo en UTC lista para una columna timestamptz de Postgres. */
    private static final SimpleDateFormat ISO_UTC =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    static {
        ISO.setTimeZone(GUAYAQUIL);
        LEGIBLE.setTimeZone(GUAYAQUIL);
        CORTA.setTimeZone(GUAYAQUIL);
        CON_HORA.setTimeZone(GUAYAQUIL);
        DDMMYYYY.setTimeZone(GUAYAQUIL);
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private FechaUtil() { }

    /** yyyy-MM-dd — el formato que viaja a Postgres. */
    public static String iso(long millis) {
        return ISO.format(new Date(millis));
    }

    public static String hoyIso() {
        return iso(System.currentTimeMillis());
    }

    /** "04 de agosto de 2026" — el formato que ve el productor. */
    public static String legible(long millis) {
        return LEGIBLE.format(new Date(millis));
    }

    public static String legibleDesdeIso(String fechaIso) {
        try {
            Date d = ISO.parse(fechaIso);
            return d == null ? fechaIso : LEGIBLE.format(d);
        } catch (Exception e) {
            return fechaIso;
        }
    }

    public static String corta(long millis) {
        return CORTA.format(new Date(millis));
    }

    public static String conHora(long millis) {
        return CON_HORA.format(new Date(millis));
    }

    public static String conHoraDesdeIsoUtc(String texto) {
        long invalido = Long.MIN_VALUE;
        long millis = millisDesdeIsoUtc(texto, invalido);
        return millis == invalido ? texto : conHora(millis);
    }

    public static long ahoraAlMinuto() {
        long ahora = System.currentTimeMillis();
        return ahora - (ahora % 60_000L);
    }

    public static Calendar calendarioGuayaquil(long millis) {
        Calendar calendario = Calendar.getInstance(GUAYAQUIL, EC);
        calendario.setTimeInMillis(millis);
        return calendario;
    }

    /**
     * "2026-08-04T17:30:00Z" — formato que viaja a Django.
     *
     * Se usa SimpleDateFormat y no java.time.Instant porque Instant exige
     * API 26 y la app soporta desde API 24 (teléfonos gama baja del recinto).
     */
    public static String isoUtc(long millis) {
        return ISO_UTC.format(new Date(millis));
    }

    /**
     * Código del muestreo al que pertenece un registro: "M-" + ddMMyyyy.
     * Por ejemplo, todo lo medido el 4 de agosto de 2026 es "M-04082026".
     *
     * Se DERIVA de la fecha de muestreo, nunca se guarda en la base local: así
     * no puede quedar desfasado si el productor corrige la fecha de un registro.
     * Agrupa por la fecha en que se MIDIÓ, no por la de subida — si mide el lunes
     * y sincroniza el miércoles, esos peces siguen siendo del muestreo del lunes,
     * que es lo que importa para analizar el crecimiento.
     *
     * En Postgres la columna homóloga es GENERATED ALWAYS a partir de
     * fecha_muestreo, de modo que servidor y app no pueden discrepar.
     */
    public static String codigoMuestreo(String fechaIso) {
        try {
            Date d = ISO.parse(fechaIso);
            return d == null ? "M-" + fechaIso : "M-" + DDMMYYYY.format(d);
        } catch (Exception e) {
            return "M-" + fechaIso;
        }
    }

    /**
     * Inverso de isoUtc: convierte lo que devuelve Postgres en epoch millis.
     *
     * PostgREST entrega timestamptz como "2026-08-04T17:30:00+00:00" o con
     * fracciones de segundo, así que se normaliza antes de parsear. Devuelve el
     * valor de respaldo si el formato no se reconoce, para que un cambio en el
     * servidor no tumbe el historial.
     */
    public static long millisDesdeIsoUtc(String texto, long respaldo) {
        if (texto == null || texto.isEmpty()) return respaldo;
        try {
            String limpio = texto.trim().replace(' ', 'T');
            int punto = limpio.indexOf('.');
            if (punto > 0) {
                // Se recorta la fracción de segundo: "…:00.123456+00:00" → "…:00+00:00"
                int fin = punto + 1;
                while (fin < limpio.length() && Character.isDigit(limpio.charAt(fin))) fin++;
                limpio = limpio.substring(0, punto) + limpio.substring(fin);
            }
            if (limpio.endsWith("Z")) {
                limpio = limpio.substring(0, limpio.length() - 1) + "+0000";
            } else if (limpio.length() > 6 && limpio.charAt(limpio.length() - 3) == ':'
                    && (limpio.charAt(limpio.length() - 6) == '+'
                     || limpio.charAt(limpio.length() - 6) == '-')) {
                limpio = limpio.substring(0, limpio.length() - 3)
                       + limpio.substring(limpio.length() - 2);
            } else {
                limpio = limpio + "+0000";
            }
            Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(limpio);
            return d == null ? respaldo : d.getTime();
        } catch (Exception e) {
            return respaldo;
        }
    }

    public static long millisDesdeIso(String fechaIso) {
        try {
            Date d = ISO.parse(fechaIso);
            return d == null ? System.currentTimeMillis() : d.getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    public static Calendar calendarioDesdeIso(String fechaIso) {
        Calendar c = Calendar.getInstance(EC);
        c.setTimeInMillis(millisDesdeIso(fechaIso));
        return c;
    }
}
