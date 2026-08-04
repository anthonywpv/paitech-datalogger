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
                    ItemHistorial.Tipo.BIOMETRIA, r.uuid,
                    "Biometría · " + r.piscina,
                    String.format(Locale.US, "%.1f g   ·   %.1f cm   ·   K %.2f",
                            r.pesoGramos, r.tallaCm, r.factorCondicion()),
                    r.fechaMuestreo, r.creadoEn, r.sincronizado));
        }

        for (RegistroAgua r : aguas) {
            if (!pasaFiltro(r.sincronizado, actual)) continue;
            String poblacion = r.poblacionEstimada == null ? ""
                    : String.format(Locale.US, "   ·   %d peces", r.poblacionEstimada);
            items.add(new ItemHistorial(
                    ItemHistorial.Tipo.AGUA, r.uuid,
                    "Agua · " + r.piscina,
                    String.format(Locale.US,
                            "pH %.1f   ·   NH4 %.2f   ·   NO2 %.2f   ·   NO3 %.1f mg/L%s",
                            r.ph, r.amonioMgL, r.nitritoMgL, r.nitratoMgL, poblacion),
                    r.fechaMuestreo, r.creadoEn, r.sincronizado));
        }

        for (EnsayoLaboratorio e : ensayos) {
            if (!pasaFiltro(e.sincronizado, actual)) continue;
            String unidad = TextUtils.isEmpty(e.unidad) ? "" : " " + e.unidad;
            items.add(new ItemHistorial(
                    ItemHistorial.Tipo.LABORATORIO, e.uuid,
                    "Laboratorio · " + e.piscina,
                    String.format(Locale.US, "%s: %.2f%s   ·   %s",
                            e.parametro, e.valor, unidad, e.tipoMuestra),
                    e.fechaMuestreo, e.creadoEn, e.sincronizado));
        }

        combinado.setValue(agruparPorMuestreo(items));
    }

    /**
     * Ordena de lo más reciente a lo más antiguo y mete un encabezado cada vez
     * que cambia el muestreo.
     *
     * El orden primario es la FECHA DE MUESTREO, no la de captura: los muestreos
     * tienen que aparecer en el orden en que ocurrieron en la piscina. Dentro de
     * cada muestreo sí manda creadoEn, para que el último pez medido quede
     * arriba y el productor vea de inmediato lo que acaba de registrar.
     */
    // Visible para el test: es Java puro y se puede cubrir sin emulador.
    static List<ItemHistorial> agruparPorMuestreo(List<ItemHistorial> items) {
        Collections.sort(items, (a, b) -> {
            int porFecha = b.fechaMuestreo.compareTo(a.fechaMuestreo);
            return porFecha != 0 ? porFecha : Long.compare(b.creadoEn, a.creadoEn);
        });

        List<ItemHistorial> conEncabezados = new ArrayList<>(items.size() + 8);
        int i = 0;
        while (i < items.size()) {
            String muestreo = items.get(i).codigoMuestreo();

            int fin = i;
            while (fin < items.size() && items.get(fin).codigoMuestreo().equals(muestreo)) {
                fin++;
            }

            conEncabezados.add(ItemHistorial.encabezado(items.get(i).fechaMuestreo, fin - i));
            conEncabezados.addAll(items.subList(i, fin));
            i = fin;
        }
        return conEncabezados;
    }

    private boolean pasaFiltro(boolean sincronizado, Filtro filtro) {
        switch (filtro) {
            case PENDIENTES:    return !sincronizado;
            case SINCRONIZADOS: return sincronizado;
            default:            return true;
        }
    }
}
