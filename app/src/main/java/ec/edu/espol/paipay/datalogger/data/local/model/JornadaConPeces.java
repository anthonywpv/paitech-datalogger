package ec.edu.espol.paipay.datalogger.data.local.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;

public class JornadaConPeces {
    @Embedded public JornadaLocal jornada;
    @Relation(parentColumn = "uuid", entityColumn = "jornadaUuid")
    public List<ObservacionPezLocal> peces;
}
