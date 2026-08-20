package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;

@Dao
public interface MovimientoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(MovimientoLocal movimiento);

    @Query("SELECT * FROM movimiento_local WHERE estadoLocal != 'ANULADO_LOCAL' ORDER BY ocurridoEn DESC")
    LiveData<List<MovimientoLocal>> observar();

    @Query("SELECT * FROM movimiento_local WHERE uuid = :uuid LIMIT 1")
    MovimientoLocal porUuid(String uuid);

    @Query("SELECT * FROM movimiento_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_EDITAR','PENDIENTE_ANULAR') ORDER BY modificadaEn")
    List<MovimientoLocal> pendientes();

    @Query("SELECT estadoLocal FROM movimiento_local WHERE uuid = :uuid")
    String estadoDe(String uuid);

    @Query("SELECT COUNT(*) FROM movimiento_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_EDITAR','PENDIENTE_ANULAR')")
    int contarPendientes();

    @Query("SELECT COUNT(*) FROM movimiento_local WHERE estadoLocal NOT IN ('SINCRONIZADO','ANULADO','ANULADO_LOCAL')")
    int contarNoResueltos();

    @Query("SELECT DISTINCT autorCorreo FROM movimiento_local WHERE estadoLocal NOT IN ('SINCRONIZADO','ANULADO','ANULADO_LOCAL')")
    List<String> autoresNoResueltos();

    @Query("SELECT COUNT(*) FROM movimiento_local WHERE estadoLocal IN ('SINCRONIZADO','ANULADO','ANULADO_LOCAL')")
    int contarSincronizados();

    @Query("DELETE FROM movimiento_local WHERE estadoLocal IN ('SINCRONIZADO','ANULADO','ANULADO_LOCAL')")
    int borrarSincronizados();

    @Query("SELECT COUNT(*) FROM movimiento_local WHERE estadoLocal NOT IN ('ANULADO','ANULADO_LOCAL') AND tipo IN ('TRASLADO','AJUSTE') AND (cicloOrigenUuid = :cicloUuid OR cicloDestinoUuid = :cicloUuid)")
    int contarTransferenciasOAjustes(String cicloUuid);

    @Query("UPDATE movimiento_local SET cicloOrigenUuid = :destinoUuid WHERE cicloOrigenUuid = :origenUuid")
    void reasignarCicloOrigen(String origenUuid, String destinoUuid);

    @Query("UPDATE movimiento_local SET cicloDestinoUuid = :destinoUuid WHERE cicloDestinoUuid = :origenUuid")
    void reasignarCicloDestino(String origenUuid, String destinoUuid);
}
