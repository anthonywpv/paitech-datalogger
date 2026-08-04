package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.Piscina;

@Dao
public interface PiscinaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarTodas(List<Piscina> piscinas);

    @Query("SELECT * FROM piscina WHERE activa = 1 ORDER BY codigo ASC")
    List<Piscina> activas();

    @Query("SELECT * FROM piscina WHERE activa = 1 ORDER BY codigo ASC")
    LiveData<List<Piscina>> observarActivas();

    @Query("SELECT COUNT(*) FROM piscina")
    int contar();
}
