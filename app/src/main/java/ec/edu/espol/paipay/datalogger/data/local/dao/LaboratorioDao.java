package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.EnsayoLaboratorio;

@Dao
public interface LaboratorioDao {

    @Insert
    long insertar(EnsayoLaboratorio ensayo);

    @Query("SELECT * FROM ensayo_laboratorio ORDER BY creado_en DESC")
    LiveData<List<EnsayoLaboratorio>> observarTodos();

    @Query("SELECT * FROM ensayo_laboratorio WHERE sincronizado = 0 ORDER BY creado_en ASC")
    List<EnsayoLaboratorio> pendientes();

    @Query("SELECT COUNT(*) FROM ensayo_laboratorio WHERE sincronizado = 0")
    int contarPendientes();

    @Query("SELECT COUNT(*) FROM ensayo_laboratorio WHERE sincronizado = 0")
    LiveData<Integer> observarPendientes();

    @Query("UPDATE ensayo_laboratorio SET sincronizado = 1, sincronizado_en = :momento WHERE uuid IN (:uuids)")
    void marcarSincronizados(List<String> uuids, long momento);
}
