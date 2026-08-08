package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.SemaforoLocal;

@Dao
public interface CatalogoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarPiscinas(List<PiscinaLocal> piscinas);

    @Query("SELECT * FROM piscina_local WHERE activa = 1 AND tipo = 'PECES' ORDER BY codigo")
    LiveData<List<PiscinaLocal>> observarPiscinasPeces();

    @Query("SELECT * FROM piscina_local WHERE activa = 1 AND tipo = 'PECES' ORDER BY codigo")
    List<PiscinaLocal> piscinasPeces();

    @Query("SELECT * FROM piscina_local WHERE uuid = :uuid LIMIT 1")
    PiscinaLocal piscina(String uuid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarSemaforos(List<SemaforoLocal> semaforos);

    @Query("SELECT * FROM semaforo_local ORDER BY piscinaCodigo")
    LiveData<List<SemaforoLocal>> observarSemaforos();
}
