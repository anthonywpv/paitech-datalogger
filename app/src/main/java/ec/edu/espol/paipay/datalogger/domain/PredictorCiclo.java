package ec.edu.espol.paipay.datalogger.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLocal;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/** Réplica offline transparente de mediana_supervivencia_v1. */
public final class PredictorCiclo {
    public static final String METODO = "mediana_supervivencia_v1";

    private PredictorCiclo() { }

    public static PrediccionCiclo calcular(int poblacionInicial, List<CicloLocal> ciclos,
                                            Set<String> excluidos, long ahoraMillis) {
        List<Double> tasas = new ArrayList<>();
        long datosHastaMillis = 0;
        for (CicloLocal ciclo : ciclos) {
            if (ciclo == null || ciclo.poblacionInicial <= 0 || ciclo.poblacionFinal == null
                    || excluidos.contains(ciclo.uuid)) continue;
            tasas.add(ciclo.poblacionFinal / (double) ciclo.poblacionInicial);
            datosHastaMillis = Math.max(datosHastaMillis, ciclo.modificadaEn);
        }
        Collections.sort(tasas);
        int n = tasas.size();
        String confianza = n == 0 ? "SIN_DATOS" : n <= 2 ? "MUY_BAJA"
                : n <= 4 ? "BAJA" : "MEDIA";
        String calculada = FechaUtil.isoUtc(ahoraMillis);
        String datosHasta = datosHastaMillis > 0
                ? FechaUtil.isoUtc(datosHastaMillis) : calculada;
        if (n == 0) {
            return new PrediccionCiclo(null, null, null, null, 0, confianza,
                    calculada, datosHasta);
        }
        double mediana = n % 2 == 1 ? tasas.get(n / 2)
                : (tasas.get(n / 2 - 1) + tasas.get(n / 2)) / 2d;
        return new PrediccionCiclo(
                (int) Math.round(poblacionInicial * mediana),
                (int) Math.round(poblacionInicial * tasas.get(0)),
                (int) Math.round(poblacionInicial * tasas.get(n - 1)),
                mediana,
                n,
                confianza,
                calculada,
                datosHasta
        );
    }
}
