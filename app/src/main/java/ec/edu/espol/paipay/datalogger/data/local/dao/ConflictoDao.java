package ec.edu.espol.paipay.datalogger.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import ec.edu.espol.paipay.datalogger.data.local.entity.ConflictoLocal;

@Dao
public interface ConflictoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(ConflictoLocal conflicto);

    @Query("SELECT * FROM conflicto_local WHERE clave = :clave LIMIT 1")
    ConflictoLocal porClave(String clave);

    @Query("DELETE FROM conflicto_local WHERE clave = :clave")
    void borrar(String clave);
}
