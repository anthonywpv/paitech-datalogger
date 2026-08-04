package ec.edu.espol.paipay.datalogger.ui.historial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pruebas del agrupado del historial por muestreo.
 *
 * Lo que se protege aquí es el ORDEN, que es lo que el productor usa para
 * comprobar de un vistazo que lo que acaba de registrar quedó guardado.
 */
public class AgrupacionHistorialTest {

    private static ItemHistorial registro(String fechaMuestreo, long creadoEn) {
        return new ItemHistorial(ItemHistorial.Tipo.BIOMETRIA, "u-" + creadoEn,
                "Biometría · P-01", "detalle", fechaMuestreo, creadoEn, false);
    }

    @Test
    public void insertaUnEncabezadoPorCadaMuestreo() {
        List<ItemHistorial> entrada = new ArrayList<>(Arrays.asList(
                registro("2026-08-04", 300),
                registro("2026-08-04", 200),
                registro("2026-08-03", 100)));

        List<ItemHistorial> salida = HistorialViewModel.agruparPorMuestreo(entrada);

        // 3 registros + 2 encabezados
        assertEquals(5, salida.size());
        assertTrue(salida.get(0).esEncabezado());
        assertEquals("M-04082026", salida.get(0).titulo);
        assertTrue(salida.get(3).esEncabezado());
        assertEquals("M-03082026", salida.get(3).titulo);
    }

    @Test
    public void elEncabezadoLlevaCuantosRegistrosAgrupa() {
        List<ItemHistorial> entrada = new ArrayList<>(Arrays.asList(
                registro("2026-08-04", 300),
                registro("2026-08-04", 200),
                registro("2026-08-03", 100)));

        List<ItemHistorial> salida = HistorialViewModel.agruparPorMuestreo(entrada);

        assertEquals("2", salida.get(0).detalle);
        assertEquals("1", salida.get(3).detalle);
    }

    @Test
    public void losMuestreosMasRecientesVanPrimero() {
        List<ItemHistorial> entrada = new ArrayList<>(Arrays.asList(
                registro("2026-08-01", 100),
                registro("2026-08-05", 200),
                registro("2026-08-03", 300)));

        List<ItemHistorial> salida = HistorialViewModel.agruparPorMuestreo(entrada);

        assertEquals("M-05082026", salida.get(0).titulo);
        assertEquals("M-03082026", salida.get(2).titulo);
        assertEquals("M-01082026", salida.get(4).titulo);
    }

    /**
     * Dentro de un muestreo manda la hora de captura: el último pez medido tiene
     * que quedar arriba, que es donde el productor lo busca al terminar.
     */
    @Test
    public void dentroDeUnMuestreoElUltimoRegistradoVaPrimero() {
        List<ItemHistorial> entrada = new ArrayList<>(Arrays.asList(
                registro("2026-08-04", 100),
                registro("2026-08-04", 300),
                registro("2026-08-04", 200)));

        List<ItemHistorial> salida = HistorialViewModel.agruparPorMuestreo(entrada);

        assertTrue(salida.get(0).esEncabezado());
        assertEquals(300L, salida.get(1).creadoEn);
        assertEquals(200L, salida.get(2).creadoEn);
        assertEquals(100L, salida.get(3).creadoEn);
    }

    /**
     * El orden primario es la fecha de MUESTREO, no la de captura. Un pez medido
     * el lunes pero tecleado el miércoles pertenece al muestreo del lunes y debe
     * aparecer debajo de los del miércoles.
     */
    @Test
    public void mandaLaFechaDeMuestreoSobreLaDeCaptura() {
        List<ItemHistorial> entrada = new ArrayList<>(Arrays.asList(
                registro("2026-08-03", 999),   // medido antes, tecleado después
                registro("2026-08-05", 100)));

        List<ItemHistorial> salida = HistorialViewModel.agruparPorMuestreo(entrada);

        assertEquals("M-05082026", salida.get(0).titulo);
        assertEquals("M-03082026", salida.get(2).titulo);
    }

    @Test
    public void registrosDeDistintoTipoDelMismoDiaCaenEnElMismoMuestreo() {
        List<ItemHistorial> entrada = new ArrayList<>(Arrays.asList(
                new ItemHistorial(ItemHistorial.Tipo.BIOMETRIA, "u-1", "Biometría", "d",
                        "2026-08-04", 200, false),
                new ItemHistorial(ItemHistorial.Tipo.AGUA, "u-2", "Agua", "d",
                        "2026-08-04", 100, true)));

        List<ItemHistorial> salida = HistorialViewModel.agruparPorMuestreo(entrada);

        assertEquals(3, salida.size());
        assertEquals("2", salida.get(0).detalle);
        assertFalse(salida.get(1).esEncabezado());
        assertFalse(salida.get(2).esEncabezado());
    }

    @Test
    public void listaVaciaNoProduceEncabezados() {
        assertTrue(HistorialViewModel.agruparPorMuestreo(new ArrayList<>()).isEmpty());
    }
}
