package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

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

    /**
     * Marca el lote como subido y deja constancia de QUIEN lo subio.
     * El correo se sella aqui y no al guardar: en campo la app no pide
     * credenciales, asi que hasta este momento no se sabia.
     */
    @Query("UPDATE ensayo_laboratorio SET sincronizado = 1, sincronizado_en = :momento, "
            + "registrado_por = :correo WHERE uuid IN (:uuids)")
    void marcarSincronizados(List<String> uuids, long momento, String correo);

    @Query("SELECT * FROM ensayo_laboratorio WHERE uuid = :uuid LIMIT 1")
    EnsayoLaboratorio porUuid(String uuid);

    @Update
    void actualizar(EnsayoLaboratorio ensayo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarOReemplazar(List<EnsayoLaboratorio> ensayos);

    @Query("SELECT uuid FROM ensayo_laboratorio")
    List<String> todosLosUuids();

    @Query("SELECT COUNT(*) FROM ensayo_laboratorio WHERE sincronizado = 1")
    int contarSincronizados();

    /**
     * Libera espacio borrando SOLO lo que ya está en la base principal.
     * El filtro sincronizado = 1 no es un detalle: es la garantía de que
     * esta operación no puede tocar un dato que el productor aún no subió.
     */
    @Query("DELETE FROM ensayo_laboratorio WHERE sincronizado = 1")
    int borrarSincronizados();
}
