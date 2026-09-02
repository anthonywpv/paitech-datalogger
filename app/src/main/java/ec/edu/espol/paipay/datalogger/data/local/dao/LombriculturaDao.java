package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.CamaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLombriculturaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroLombriculturaLocal;

@Dao
public interface LombriculturaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarCamas(List<CamaLocal> camas);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarCama(CamaLocal cama);

    @Query("SELECT * FROM cama_local WHERE activa = 1 ORDER BY codigo")
    LiveData<List<CamaLocal>> observarCamas();

    @Query("SELECT * FROM cama_local WHERE activa = 1 ORDER BY codigo")
    List<CamaLocal> camas();

    @Query("SELECT * FROM cama_local WHERE uuid = :uuid LIMIT 1")
    CamaLocal cama(String uuid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarCiclo(CicloLombriculturaLocal ciclo);

    @Query("SELECT * FROM ciclo_lombricultura_local WHERE camaUuid = :camaUuid AND estado = 'ACTIVO' LIMIT 1")
    CicloLombriculturaLocal cicloActivo(String camaUuid);

    @Query("SELECT * FROM ciclo_lombricultura_local WHERE uuid = :uuid LIMIT 1")
    CicloLombriculturaLocal ciclo(String uuid);

    @Query("SELECT COALESCE(MAX(numero), 0) FROM ciclo_lombricultura_local WHERE camaUuid = :camaUuid")
    int maxNumeroCiclo(String camaUuid);

    @Query("SELECT * FROM ciclo_lombricultura_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_CERRAR','PENDIENTE_CREAR_Y_CERRAR') ORDER BY modificadaEn")
    List<CicloLombriculturaLocal> ciclosPendientes();

    @Query("SELECT estadoLocal FROM ciclo_lombricultura_local WHERE uuid = :uuid")
    String estadoLocalCiclo(String uuid);

    @Query("DELETE FROM ciclo_lombricultura_local WHERE uuid = :uuid")
    void borrarCiclo(String uuid);

    @Query("SELECT DISTINCT autorCorreo FROM ciclo_lombricultura_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_CERRAR','PENDIENTE_CREAR_Y_CERRAR','CONFLICTO')")
    List<String> autoresCiclosNoResueltos();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarRegistro(RegistroLombriculturaLocal registro);

    @Query("SELECT * FROM registro_lombricultura_local ORDER BY capturadaEn DESC")
    LiveData<List<RegistroLombriculturaLocal>> observarRegistros();

    @Query("SELECT * FROM registro_lombricultura_local WHERE uuid = :uuid LIMIT 1")
    RegistroLombriculturaLocal registro(String uuid);

    @Query("SELECT * FROM registro_lombricultura_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_EDITAR','PENDIENTE_ANULAR') ORDER BY modificadaEn")
    List<RegistroLombriculturaLocal> registrosPendientes();

    @Query("SELECT estadoLocal FROM registro_lombricultura_local WHERE uuid = :uuid")
    String estadoLocalRegistro(String uuid);

    @Query("UPDATE registro_lombricultura_local SET cicloUuid = :nuevo WHERE cicloUuid = :anterior")
    void reasignarCiclo(String anterior, String nuevo);

    @Query("SELECT DISTINCT autorCorreo FROM registro_lombricultura_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_EDITAR','PENDIENTE_ANULAR','CONFLICTO')")
    List<String> autoresNoResueltos();

    @Query("SELECT COUNT(*) FROM registro_lombricultura_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_EDITAR','PENDIENTE_ANULAR','CONFLICTO')")
    int contarRegistrosNoResueltos();

    @Query("SELECT COUNT(*) FROM ciclo_lombricultura_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_CERRAR','PENDIENTE_CREAR_Y_CERRAR','CONFLICTO')")
    int contarCiclosNoResueltos();

    @Query("DELETE FROM registro_lombricultura_local WHERE estadoLocal IN ('SINCRONIZADO','ANULADO')")
    int borrarRegistrosResueltos();

    @Query("SELECT COUNT(*) FROM registro_lombricultura_local WHERE estadoLocal IN ('SINCRONIZADO','ANULADO')")
    int contarRegistrosResueltos();

    @Query("SELECT COUNT(*) FROM ciclo_lombricultura_local WHERE estadoLocal = 'SINCRONIZADO'")
    int contarCiclosSincronizados();

    @Query("DELETE FROM ciclo_lombricultura_local WHERE estado = 'CERRADO' AND estadoLocal = 'SINCRONIZADO'")
    int borrarCiclosCerradosSincronizados();
}
