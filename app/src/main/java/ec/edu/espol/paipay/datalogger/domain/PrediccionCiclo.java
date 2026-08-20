package ec.edu.espol.paipay.datalogger.domain;

public class PrediccionCiclo {
    public final Integer estimacion;
    public final Integer minimo;
    public final Integer maximo;
    public final Double tasa;
    public final int ciclosUsados;
    public final String confianza;
    public final String calculadaEn;
    public final String datosHasta;

    public PrediccionCiclo(Integer estimacion, Integer minimo, Integer maximo,
                           Double tasa, int ciclosUsados, String confianza,
                           String calculadaEn, String datosHasta) {
        this.estimacion = estimacion;
        this.minimo = minimo;
        this.maximo = maximo;
        this.tasa = tasa;
        this.ciclosUsados = ciclosUsados;
        this.confianza = confianza;
        this.calculadaEn = calculadaEn;
        this.datosHasta = datosHasta;
    }
}
