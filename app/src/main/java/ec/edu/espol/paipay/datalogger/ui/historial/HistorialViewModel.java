package ec.edu.espol.paipay.datalogger.ui.historial;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MediatorLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;
import ec.edu.espol.paipay.datalogger.data.repo.MovimientoRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.RegistroRepositorio;
import ec.edu.espol.paipay.datalogger.data.repo.SesionManager;

/** Combina la cronología comunitaria y marca como editables solo los registros propios. */
public class HistorialViewModel extends AndroidViewModel {
    public enum Filtro { TODOS, PENDIENTES, SINCRONIZADOS }

    private final MediatorLiveData<List<ItemHistorial>> salida = new MediatorLiveData<>();
    private List<JornadaConPeces> jornadas = new ArrayList<>();
    private List<MovimientoLocal> movimientos = new ArrayList<>();
    private List<PiscinaLocal> piscinas = new ArrayList<>();
    private Filtro filtro = Filtro.TODOS;
    private final String correoSesion;

    public HistorialViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
        RegistroRepositorio registros = new RegistroRepositorio(aplicacion);
        MovimientoRepositorio movimientosRepositorio = new MovimientoRepositorio(aplicacion);
        correoSesion = SesionManager.obtener(aplicacion).getUsuario();
        salida.addSource(registros.jornadas(), lista -> {
            jornadas = lista == null ? new ArrayList<>() : lista;
            recalcular();
        });
        salida.addSource(movimientosRepositorio.movimientos(), lista -> {
            movimientos = lista == null ? new ArrayList<>() : lista;
            recalcular();
        });
        salida.addSource(registros.piscinas(), lista -> {
            piscinas = lista == null ? new ArrayList<>() : lista;
            recalcular();
        });
    }

    public MediatorLiveData<List<ItemHistorial>> historial() { return salida; }
    public Filtro filtroActual() { return filtro; }
    public void cambiarFiltro(Filtro filtro) {
        this.filtro = filtro;
        recalcular();
    }

    private void recalcular() {
        List<ItemHistorial> items = new ArrayList<>();
        for (JornadaConPeces dato : jornadas) {
            JornadaLocal jornada = dato.jornada;
            boolean sincronizado = resuelto(jornada.estadoLocal);
            if (!aceptaFiltro(sincronizado)) continue;
            int peces = dato.peces == null ? 0 : dato.peces.size();
            String bloques = jornada.incluyeAgua && peces > 0 ? "Agua y biometría"
                    : jornada.incluyeAgua ? "Solo agua" : "Solo biometría";
            String detalle = bloques + " · población "
                    + (jornada.poblacionEstimada == null ? "sin completar" : jornada.poblacionEstimada)
                    + (peces > 0
                    ? String.format(Locale.getDefault(), " · %d peces medidos", peces) : "")
                    + autor(jornada.autorCorreo)
                    + sufijoEstado(jornada.estadoLocal);
            items.add(new ItemHistorial(ItemHistorial.Tipo.JORNADA, jornada.uuid,
                    "Jornada · " + jornada.piscinaCodigo, detalle, jornada.capturadaEn,
                    jornada.modificadaEn, sincronizado, jornada.estadoLocal,
                    jornada.autorCorreo, esAutor(jornada.autorCorreo)));
        }

        Map<String, String> codigos = new HashMap<>();
        for (PiscinaLocal piscina : piscinas) codigos.put(piscina.uuid, piscina.codigo);
        for (MovimientoLocal movimiento : movimientos) {
            boolean sincronizado = resuelto(movimiento.estadoLocal);
            if (!aceptaFiltro(sincronizado)) continue;
            String origen = codigo(codigos, movimiento.piscinaOrigenUuid);
            String destino = codigo(codigos, movimiento.piscinaDestinoUuid);
            String recorrido = origen != null && destino != null ? origen + " → " + destino
                    : origen != null ? "sale de " + origen : "entra a " + destino;
            String detalle = movimiento.cantidad + " peces · " + recorrido
                    + autor(movimiento.autorCorreo)
                    + sufijoEstado(movimiento.estadoLocal);
            items.add(new ItemHistorial(ItemHistorial.Tipo.MOVIMIENTO, movimiento.uuid,
                    etiquetaTipo(movimiento.tipo), detalle, movimiento.ocurridoEn,
                    movimiento.modificadaEn, sincronizado, movimiento.estadoLocal,
                    movimiento.autorCorreo, esAutor(movimiento.autorCorreo)));
        }

        items.sort((a, b) -> valor(b.fechaMuestreo).compareTo(valor(a.fechaMuestreo)));
        salida.setValue(items);
    }

    private boolean aceptaFiltro(boolean sincronizado) {
        if (filtro == Filtro.PENDIENTES) return !sincronizado;
        if (filtro == Filtro.SINCRONIZADOS) return sincronizado;
        return true;
    }

    private boolean resuelto(String estado) {
        return JornadaLocal.SINCRONIZADO.equals(estado)
                || JornadaLocal.ANULADO.equals(estado)
                || JornadaLocal.ANULADO_LOCAL.equals(estado);
    }

    private String sufijoEstado(String estado) {
        if (JornadaLocal.CONFLICTO.equals(estado)) return " · CONFLICTO";
        if (JornadaLocal.BORRADOR.equals(estado)) return " · BORRADOR";
        if (JornadaLocal.ANULADO.equals(estado) || JornadaLocal.ANULADO_LOCAL.equals(estado)) {
            return " · ANULADO";
        }
        return "";
    }

    private String codigo(Map<String, String> codigos, String uuid) {
        if (uuid == null) return null;
        String codigo = codigos.get(uuid);
        return codigo == null ? "piscina " + uuid.substring(0, Math.min(8, uuid.length())) : codigo;
    }

    private String etiquetaTipo(String tipo) {
        if ("SIEMBRA".equals(tipo)) return "Siembra";
        if ("MORTALIDAD".equals(tipo)) return "Mortalidad";
        if ("COSECHA_VENTA".equals(tipo)) return "Cosecha o venta";
        if ("TRASLADO".equals(tipo)) return "Traslado";
        if ("ESCAPE".equals(tipo)) return "Escape";
        return "Ajuste de población";
    }

    private String valor(String texto) { return texto == null ? "" : texto; }

    private boolean esAutor(String correo) {
        return correo != null && correoSesion != null
                && correo.equalsIgnoreCase(correoSesion);
    }

    private String autor(String correo) {
        return correo == null || correo.trim().isEmpty() ? "" : " · por " + correo;
    }
}
