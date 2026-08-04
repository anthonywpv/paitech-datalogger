package ec.edu.espol.paipay.datalogger.ui.registro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ec.edu.espol.paipay.datalogger.databinding.FragmentFormLombriculturaBinding;

/**
 * Lombricultura: sección declarada, registro todavía por definir.
 *
 * Los lechos LOM-01 y LOM-02 ya existen en el catálogo de piscinas
 * (ver la semilla de PaipayDatabase), pero aún no se ha acordado con el equipo
 * de Acuicultura qué se mide en ellos: humedad y temperatura del lecho,
 * alimento aportado, lombriz cosechada, o alguna combinación.
 *
 * Se deja la pestaña visible en lugar de ocultarla para que quede constancia
 * de que la app contempla el componente, y para que añadirlo más adelante sea
 * rellenar este hueco y no rehacer la navegación.
 *
 * Al implementarla, seguir el patrón de los otros tres formularios: extender
 * FormularioBase (que ya trae fecha, piscina, validación y modo corrección),
 * crear entidad + DAO + DTO, y añadir el bloque correspondiente en
 * SincronizacionRepositorio. La lista completa está en el LEEME.
 */
public class LombriculturaFragment extends Fragment {

    private FragmentFormLombriculturaBinding vista;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle savedInstanceState) {
        vista = FragmentFormLombriculturaBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }
}
