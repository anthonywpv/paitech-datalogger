package ec.edu.espol.paipay.datalogger.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLocal;

public class PredictorCicloTest {
    private static final Instant AHORA = Instant.parse("2026-08-20T15:00:00Z");

    @Test public void sinHistorialNoInventaUnaCifra() {
        PrediccionCiclo resultado = PredictorCiclo.calcular(
                200, Collections.emptyList(), Collections.emptySet(), AHORA.toEpochMilli());

        assertNull(resultado.estimacion);
        assertEquals(0, resultado.ciclosUsados);
        assertEquals("SIN_DATOS", resultado.confianza);
    }

    @Test public void usaMedianaYRangoDeCiclosComparables() {
        CicloLocal a = cerrado("a", 100, 80, 1);
        CicloLocal b = cerrado("b", 100, 60, 2);
        CicloLocal c = cerrado("c", 100, 90, 3);

        PrediccionCiclo resultado = PredictorCiclo.calcular(
                200, Arrays.asList(a, b, c), Collections.emptySet(), AHORA.toEpochMilli());

        assertEquals(Integer.valueOf(160), resultado.estimacion);
        assertEquals(Integer.valueOf(120), resultado.minimo);
        assertEquals(Integer.valueOf(180), resultado.maximo);
        assertEquals(3, resultado.ciclosUsados);
        assertEquals("BAJA", resultado.confianza);
    }

    @Test public void excluyeCiclosConTrasladoOAjuste() {
        CicloLocal comparable = cerrado("comparable", 100, 80, 1);
        CicloLocal excluido = cerrado("excluido", 100, 10, 2);

        PrediccionCiclo resultado = PredictorCiclo.calcular(
                200, Arrays.asList(comparable, excluido),
                new HashSet<>(Collections.singletonList("excluido")),
                AHORA.toEpochMilli());

        assertEquals(Integer.valueOf(160), resultado.estimacion);
        assertEquals(1, resultado.ciclosUsados);
    }

    private CicloLocal cerrado(String uuid, int inicial, int finalVivo, long modificada) {
        CicloLocal ciclo = new CicloLocal();
        ciclo.uuid = uuid;
        ciclo.poblacionInicial = inicial;
        ciclo.poblacionFinal = finalVivo;
        ciclo.modificadaEn = modificada;
        return ciclo;
    }
}
