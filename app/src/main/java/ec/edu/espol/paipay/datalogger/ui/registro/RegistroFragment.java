package ec.edu.espol.paipay.datalogger.ui.registro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.databinding.FragmentRegistroBinding;

/**
 * Sección "Registrar": agrupa las tres fuentes de datos del proyecto.
 *
 *   Pestaña 1 — Peces:       biometría in situ (peso y talla).
 *   Pestaña 2 — Agua:        calidad de agua in situ (alimenta el semáforo).
 *   Pestaña 3 — Laboratorio: resultados analíticos externos.
 */
public class RegistroFragment extends Fragment {

    private FragmentRegistroBinding vista;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor,
                             @Nullable Bundle savedInstanceState) {
        vista = FragmentRegistroBinding.inflate(inflater, contenedor, false);
        return vista.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View raiz, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(raiz, savedInstanceState);

        vista.paginador.setAdapter(new AdaptadorPestanas(this));
        vista.paginador.setOffscreenPageLimit(2);

        new TabLayoutMediator(vista.pestanas, vista.paginador, (pestana, posicion) -> {
            switch (posicion) {
                case 0:
                    pestana.setText(R.string.tab_biometria);
                    pestana.setIcon(R.drawable.ic_pez);
                    break;
                case 1:
                    pestana.setText(R.string.tab_agua);
                    pestana.setIcon(R.drawable.ic_gota);
                    break;
                default:
                    pestana.setText(R.string.tab_laboratorio);
                    pestana.setIcon(R.drawable.ic_laboratorio);
                    break;
            }
        }).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        vista = null;
    }

    private static class AdaptadorPestanas extends FragmentStateAdapter {

        AdaptadorPestanas(@NonNull Fragment padre) {
            super(padre);
        }

        @NonNull
        @Override
        public Fragment createFragment(int posicion) {
            switch (posicion) {
                case 0:  return new BiometriaFragment();
                case 1:  return new AguaFragment();
                default: return new LaboratorioFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
