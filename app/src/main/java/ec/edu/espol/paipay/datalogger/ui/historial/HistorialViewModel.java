package ec.edu.espol.paipay.datalogger.ui.historial;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ec.edu.espol.paipay.datalogger.data.local.entity.EnsayoLaboratorio;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroAgua;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroBiometria;
import ec.edu.espol.paipay.datalogger.data.repo.RegistroRepositorio;

/**
 * Une las tres tablas locales en una sola lista ordenada por fecha de captura
 * y aplica el filtro elegido por el usuario (todos / pendientes / sincronizados).
 */
public class HistorialViewModel extends AndroidViewModel {

    public enum Filtro { TODOS, PENDIENTES, SINCRONIZADOS }

    private final MediatorLiveData<List<ItemHistorial>> combinado = new MediatorLiveData<>();
    private final MutableLiveData<Filtro> filtro = new MutableLiveData<>(Filtro.TODOS);

    private List<RegistroBiometria> biometrias = new ArrayList<>();
    private List<RegistroAgua> aguas = new ArrayList<>();
    private List<EnsayoLaboratorio> ensayos = new ArrayList<>();

    public HistorialViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
        RegistroRepositorio repositorio = new RegistroRepositorio(aplicacion);

        combinado.addSource(repositorio.biometrias(), lista -> {
            biometrias = lista == null ? new ArrayList<>() : lista;
            recomponer();
        });
        combinado.addSource(repositorio.aguas(), lista -> {
            aguas = lista == null ? new ArrayList<>() : lista;
            recomponer();
        });
        combinado.addSource(repositorio.ensayos(), lista -> {
            ensayos = lista == null ? new ArrayList<>() : lista;
            recomponer();
        });
        combinado.addSource(filtro, f -> recomponer());
    }

    public LiveData<List<ItemHistorial>> historial() {
        return combinado;
    }

    public void cambiarFiltro(Filtro nuevo) {
        filtro.setValue(nuevo);
    }

    private void recomponer() {
        Filtro actual = filtro.getValue() == null ? Filtro.TODOS : filtro.getValue();
        List<ItemHistorial> items = new ArrayList<>();

        for (RegistroBiometria r : biometrias) {
            if (!pasaFiltro(r.sincronizado, actual)) continue;
            items.add(new ItemHistorial(
                    ItemHistorial.Tipo.BIOMETRIA,
                    "Biometría · " + r.piscina,
                    String.format(Locale.US, "%.1f g   ·   %.1f cm   ·   %d pez(ces)",
                            r.pesoGramos, r.tallaCm, r.cantidadMuestreada),
                    r.fechaMuestreo, r.creadoEn, r.sincronizado));
        }

        for (RegistroAgua r : aguas) {
            if (!pasaFiltro(r.sincronizado, actual)) continue;
            String ph = r.ph == null ? "" : String.format(Locale.US, "   ·   pH %.1f", r.ph);
            items.add(new ItemHistorial(
                    ItemHistorial.Tipo.AGUA,
                    "Agua · " + r.piscina,
                    String.format(Locale.US, "%.1f °C   ·   %.1f mg/L%s",
                            r.temperaturaC, r.oxigenoMgL, ph),
                    r.fechaMuestreo, r.creadoEn, r.sincronizado));
        }

        for (EnsayoLaboratorio e : ensayos) {
            if (!pasaFiltro(e.sincronizado, actual)) continue;
            String unidad = TextUtils.isEmpty(e.unidad) ? "" : " " + e.unidad;
            items.add(new ItemHistorial(
                    ItemHistorial.Tipo.LABORATORIO,
                    "Laboratorio · " + e.piscina,
                    String.format(Locale.US, "%s: %.2f%s   ·   %s",
                            e.parametro, e.valor, unidad, e.tipoMuestra),
                    e.fechaMuestreo, e.creadoEn, e.sincronizado));
        }

        Collections.sort(items, (a, b) -> Long.compare(b.creadoEn, a.creadoEn));
        combinado.setValue(items);
    }

    private boolean pasaFiltro(boolean sincronizado, Filtro filtro) {
        switch (filtro) {
            case PENDIENTES:    return !sincronizado;
            case SINCRONIZADOS: return sincronizado;
            default:            return true;
        }
    }
}
