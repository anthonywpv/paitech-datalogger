package ec.edu.espol.paipay.datalogger.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Formatos de fecha unificados para toda la app (zona horaria de Ecuador). */
public final class FechaUtil {

    public static final Locale EC = new Locale("es", "EC");

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", EC);
    private static final SimpleDateFormat LEGIBLE = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", EC);
    private static final SimpleDateFormat CORTA = new SimpleDateFormat("dd/MM/yyyy", EC);
    private static final SimpleDateFormat CON_HORA = new SimpleDateFormat("dd/MM/yyyy HH:mm", EC);

    /** Marca de tiempo en UTC lista para una columna timestamptz de Postgres. */
    private static final SimpleDateFormat ISO_UTC =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    static {
        ISO_UTC.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
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

    /**
     * "2026-08-04T17:30:00Z" — formato que viaja a Neon.
     *
     * Se usa SimpleDateFormat y no java.time.Instant porque Instant exige
     * API 26 y la app soporta desde API 24 (teléfonos gama baja del recinto).
     */
    public static String isoUtc(long millis) {
        return ISO_UTC.format(new Date(millis));
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
