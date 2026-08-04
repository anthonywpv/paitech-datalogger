package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;

@Dao
public interface BiometriaDao {

    @Insert
    long insertar(RegistroBiometria registro);

    @Query("SELECT * FROM registro_biometria ORDER BY creado_en DESC")
    LiveData<List<RegistroBiometria>> observarTodos();

    @Query("SELECT * FROM registro_biometria WHERE sincronizado = 0 ORDER BY creado_en ASC")
    List<RegistroBiometria> pendientes();

    @Query("SELECT COUNT(*) FROM registro_biometria WHERE sincronizado = 0")
    int contarPendientes();

    @Query("SELECT COUNT(*) FROM registro_biometria WHERE sincronizado = 0")
    LiveData<Integer> observarPendientes();

    /**
     * Marca el lote como subido y deja constancia de QUIEN lo subio.
     * El correo se sella aqui y no al guardar: en campo la app no pide
     * credenciales, asi que hasta este momento no se sabia.
     */
    @Query("UPDATE registro_biometria SET sincronizado = 1, sincronizado_en = :momento, "
            + "registrado_por = :correo WHERE uuid IN (:uuids)")
    void marcarSincronizados(List<String> uuids, long momento, String correo);

    @Query("SELECT * FROM registro_biometria WHERE piscina = :piscina ORDER BY fecha_muestreo DESC")
    List<RegistroBiometria> porPiscina(String piscina);

    /**
     * Cuántos peces se llevan medidos en el muestreo en curso (misma fecha y
     * misma piscina). Alimenta el contador que ve el productor mientras mide.
     */
    @Query("SELECT COUNT(*) FROM registro_biometria "
            + "WHERE fecha_muestreo = :fecha AND piscina = :piscina")
    int contarEnMuestreo(String fecha, String piscina);

    @Query("SELECT * FROM registro_biometria WHERE uuid = :uuid LIMIT 1")
    RegistroBiometria porUuid(String uuid);

    @Update
    void actualizar(RegistroBiometria registro);

    /**
     * Alta o reemplazo por uuid. Es lo que usa el refresco del historial para
     * fusionar lo que devuelve Neon sin duplicar lo que ya está en el teléfono.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarOReemplazar(List<RegistroBiometria> registros);

    @Query("SELECT uuid FROM registro_biometria")
    List<String> todosLosUuids();
}
