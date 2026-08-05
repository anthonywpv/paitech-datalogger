package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;

@Dao
public interface AguaDao {

    @Insert
    long insertar(RegistroAgua registro);

    @Query("SELECT * FROM registro_agua ORDER BY creado_en DESC")
    LiveData<List<RegistroAgua>> observarTodos();

    @Query("SELECT * FROM registro_agua WHERE sincronizado = 0 ORDER BY creado_en ASC")
    List<RegistroAgua> pendientes();

    @Query("SELECT COUNT(*) FROM registro_agua WHERE sincronizado = 0")
    int contarPendientes();

    @Query("SELECT COUNT(*) FROM registro_agua WHERE sincronizado = 0")
    LiveData<Integer> observarPendientes();

    /**
     * Marca el lote como subido y deja constancia de QUIEN lo subio.
     * El correo se sella aqui y no al guardar: en campo la app no pide
     * credenciales, asi que hasta este momento no se sabia.
     */
    @Query("UPDATE registro_agua SET sincronizado = 1, sincronizado_en = :momento, "
            + "registrado_por = :correo WHERE uuid IN (:uuids)")
    void marcarSincronizados(List<String> uuids, long momento, String correo);

    /**
     * Última medición de cada piscina. Es la consulta que alimenta el
     * Semáforo de Alertas: una fila por piscina, la más reciente.
     */
    @Query("SELECT * FROM registro_agua r WHERE r.creado_en = " +
           "(SELECT MAX(r2.creado_en) FROM registro_agua r2 WHERE r2.piscina = r.piscina) " +
           "GROUP BY r.piscina ORDER BY r.piscina ASC")
    LiveData<List<RegistroAgua>> observarUltimaPorPiscina();

    @Query("SELECT * FROM registro_agua WHERE uuid = :uuid LIMIT 1")
    RegistroAgua porUuid(String uuid);

    @Update
    void actualizar(RegistroAgua registro);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarOReemplazar(List<RegistroAgua> registros);

    @Query("SELECT uuid FROM registro_agua")
    List<String> todosLosUuids();

    @Query("SELECT COUNT(*) FROM registro_agua WHERE sincronizado = 1")
    int contarSincronizados();

    /**
     * Libera espacio borrando SOLO lo que ya está en la base principal.
     * El filtro sincronizado = 1 no es un detalle: es la garantía de que
     * esta operación no puede tocar un dato que el productor aún no subió.
     */
    @Query("DELETE FROM registro_agua WHERE sincronizado = 1")
    int borrarSincronizados();
}
