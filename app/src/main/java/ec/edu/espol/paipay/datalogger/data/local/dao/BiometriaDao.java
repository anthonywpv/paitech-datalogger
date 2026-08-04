package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

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

    @Query("UPDATE registro_biometria SET sincronizado = 1, sincronizado_en = :momento WHERE uuid IN (:uuids)")
    void marcarSincronizados(List<String> uuids, long momento);

    @Query("SELECT * FROM registro_biometria WHERE piscina = :piscina ORDER BY fecha_muestreo DESC")
    List<RegistroBiometria> porPiscina(String piscina);
}
