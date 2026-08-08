package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;

@Dao
public abstract class JornadaDao {
    @Transaction
    @Query("SELECT * FROM jornada_local WHERE autorCorreo = :correo AND estadoLocal != 'ANULADO_LOCAL' ORDER BY capturadaEn DESC, modificadaEn DESC")
    public abstract LiveData<List<JornadaConPeces>> observarHistorial(String correo);

    @Transaction
    @Query("SELECT * FROM jornada_local WHERE uuid = :uuid LIMIT 1")
    public abstract JornadaConPeces porUuid(String uuid);

    @Transaction
    @Query("SELECT * FROM jornada_local WHERE autorCorreo = :correo AND estadoLocal = 'BORRADOR' ORDER BY modificadaEn DESC LIMIT 1")
    public abstract JornadaConPeces ultimoBorrador(String correo);

    @Transaction
    @Query("SELECT * FROM jornada_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_EDITAR','PENDIENTE_ANULAR') ORDER BY modificadaEn")
    public abstract List<JornadaConPeces> pendientes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertarJornada(JornadaLocal jornada);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertarPeces(List<ObservacionPezLocal> peces);

    @Query("DELETE FROM observacion_pez_local WHERE jornadaUuid = :jornadaUuid")
    abstract void borrarPeces(String jornadaUuid);

    @Transaction
    public void guardar(JornadaLocal jornada, List<ObservacionPezLocal> peces) {
        insertarJornada(jornada);
        borrarPeces(jornada.uuid);
        if (peces != null && !peces.isEmpty()) insertarPeces(peces);
    }

    @Query("SELECT estadoLocal FROM jornada_local WHERE uuid = :uuid")
    public abstract String estadoDe(String uuid);

    @Query("SELECT COUNT(*) FROM jornada_local WHERE estadoLocal IN ('PENDIENTE_CREAR','PENDIENTE_EDITAR','PENDIENTE_ANULAR')")
    public abstract int contarPendientes();

    @Query("SELECT COUNT(*) FROM jornada_local WHERE estadoLocal NOT IN ('SINCRONIZADO','ANULADO','ANULADO_LOCAL')")
    public abstract int contarNoResueltas();

    @Query("SELECT DISTINCT autorCorreo FROM jornada_local WHERE estadoLocal NOT IN ('SINCRONIZADO','ANULADO','ANULADO_LOCAL')")
    public abstract List<String> autoresNoResueltos();

    @Query("SELECT COUNT(*) FROM jornada_local WHERE estadoLocal IN ('SINCRONIZADO','ANULADO','ANULADO_LOCAL')")
    public abstract int contarSincronizados();

    @Query("DELETE FROM jornada_local WHERE estadoLocal IN ('SINCRONIZADO','ANULADO','ANULADO_LOCAL')")
    public abstract int borrarSincronizados();
}
