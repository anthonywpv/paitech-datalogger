package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLocal;

@Dao
public interface CicloDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(CicloLocal ciclo);

    @Query("SELECT * FROM ciclo_local ORDER BY iniciadoEn DESC")
    LiveData<List<CicloLocal>> observarTodos();

    @Query("SELECT * FROM ciclo_local WHERE piscinaUuid = :piscinaUuid AND estado = 'ACTIVO' LIMIT 1")
    CicloLocal activo(String piscinaUuid);

    @Query("SELECT * FROM ciclo_local WHERE uuid = :uuid LIMIT 1")
    CicloLocal porUuid(String uuid);

    @Query("SELECT * FROM ciclo_local WHERE piscinaUuid = :piscinaUuid AND estado = 'CERRADO' AND estadoLocal = 'SINCRONIZADO' ORDER BY cerradoEn")
    List<CicloLocal> cerradosParaPrediccion(String piscinaUuid);

    @Query("SELECT COALESCE(MAX(numero), 0) FROM ciclo_local WHERE piscinaUuid = :piscinaUuid")
    int maxNumero(String piscinaUuid);

    @Query("SELECT * FROM ciclo_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_CERRAR','PENDIENTE_CREAR_Y_CERRAR') ORDER BY modificadaEn")
    List<CicloLocal> pendientes();

    @Query("SELECT estadoLocal FROM ciclo_local WHERE uuid = :uuid")
    String estadoLocalDe(String uuid);

    @Query("SELECT COUNT(*) FROM ciclo_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_CERRAR','PENDIENTE_CREAR_Y_CERRAR','CONFLICTO')")
    int contarNoResueltos();

    @Query("SELECT COUNT(*) FROM ciclo_local WHERE estadoLocal = 'SINCRONIZADO'")
    int contarSincronizados();

    @Query("DELETE FROM ciclo_local WHERE estadoLocal = 'SINCRONIZADO' AND estado = 'CERRADO'")
    int borrarCerradosSincronizados();

    @Query("DELETE FROM ciclo_local WHERE uuid = :uuid")
    void borrar(String uuid);
}
