package ec.edu.espol.paipay.datalogger.data.remote.dto;

import java.util.List;

public class SemaforoApiDto {
    public PiscinaResumenDto piscina;
    public JornadaApiDto jornada;
    public ResultadoDto semaforo;

    public static class PiscinaResumenDto {
        public String id;
        public String codigo;
        public String nombre;
    }

    public static class ResultadoDto {
        public String estado;
        public List<LecturaDto> lecturas;
        public boolean umbralesProvisionales;
    }

    public static class LecturaDto {
        public String parametro;
        public String valor;
        public String unidad;
        public String estado;
        public String diagnostico;
        public String recomendacion;
    }
}
