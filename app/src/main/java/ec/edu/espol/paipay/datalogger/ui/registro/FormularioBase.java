package ec.edu.espol.paipay.datalogger.ui.registro;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import ec.edu.espol.paipay.datalogger.R;
import ec.edu.espol.paipay.datalogger.data.local.entity.Piscina;
import ec.edu.espol.paipay.datalogger.data.repo.RegistroRepositorio;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

/**
 * Comportamiento compartido por los tres formularios de registro.
 *
 * Concentra las decisiones de usabilidad pensadas para el productor:
 *   · La fecha se elige con calendario, nunca se escribe a mano.
 *   · La piscina se elige de una lista, nunca se escribe (evita "P1", "p-1", "Piscina1").
 *   · Los errores se muestran en el campo mismo, en lenguaje llano.
 *   · Al guardar aparece una confirmación grande y clara.
 */
public abstract class FormularioBase extends Fragment {

    /** Clave del uuid a corregir. Ausente = alta de un registro nuevo. */
    protected static final String ARG_UUID = "uuid";

    protected RegistroRepositorio repositorio;
    /** Fecha seleccionada en formato yyyy-MM-dd. */
    protected String fechaIso = FechaUtil.hoyIso();

    /** null = alta; con valor = corrección de un registro existente. */
    protected String uuidEnEdicion;

    /** Argumentos para abrir cualquiera de los tres formularios en modo corrección. */
    protected static Bundle argumentosDeEdicion(String uuid) {
        Bundle args = new Bundle();
        args.putString(ARG_UUID, uuid);
        return args;
    }

    @Override
    public void onAttach(@NonNull Context contexto) {
        super.onAttach(contexto);
        repositorio = new RegistroRepositorio(contexto);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uuidEnEdicion = getArguments() == null ? null : getArguments().getString(ARG_UUID);
    }

    protected boolean estaEditando() {
        return uuidEnEdicion != null;
    }

    /** Vuelve a la pantalla desde la que se abrió la corrección. */
    protected void cerrarEdicion() {
        if (isAdded()) getParentFragmentManager().popBackStack();
    }

    // ---------------------- FECHA ----------------------

    protected void configurarSelectorFecha(TextInputEditText campo) {
        campo.setText(FechaUtil.legibleDesdeIso(fechaIso));
        campo.setOnClickListener(v -> {
            MaterialDatePicker<Long> selector = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.campo_fecha)
                    .setSelection(FechaUtil.millisDesdeIso(fechaIso))
                    .build();
            selector.addOnPositiveButtonClickListener(millis -> {
                fechaIso = FechaUtil.iso(millis);
                campo.setText(FechaUtil.legibleDesdeIso(fechaIso));
                onFechaCambiada();
            });
            selector.show(getChildFragmentManager(), "selector_fecha");
        });
    }

    /**
     * Gancho para los formularios que muestran algo dependiente de la fecha.
     * Biometría lo usa para recontar los peces del muestreo al cambiar de día.
     */
    protected void onFechaCambiada() { }

    // ---------------------- PISCINAS ----------------------

    protected void cargarPiscinas(MaterialAutoCompleteTextView campo) {
        repositorio.piscinas().observe(getViewLifecycleOwner(), lista -> {
            if (lista == null || lista.isEmpty()) return;
            List<String> nombres = new ArrayList<>(lista.size());
            for (Piscina p : lista) nombres.add(p.toString());
            campo.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, nombres));
            campo.setThreshold(0);
        });
    }

    /**
     * De "P-01 · Piscina 1 - Engorde" extrae "P-01".
     *
     * Guardar el código y no el nombre largo evita que la base principal
     * termine con "P1", "p-1", "Piscina 1" y "piscina uno" como si fueran
     * piscinas distintas.
     */
    protected String codigoDePiscina(String textoSeleccionado) {
        if (TextUtils.isEmpty(textoSeleccionado)) return "";
        int separador = textoSeleccionado.indexOf(" \u00b7 ");
        if (separador > 0) {
            return textoSeleccionado.substring(0, separador).trim();
        }
        return textoSeleccionado.trim();
    }

    // ---------------------- VALIDACIÓN ----------------------

    protected String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    protected String texto(MaterialAutoCompleteTextView campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    protected boolean exigir(TextInputLayout contenedor, String valor) {
        if (TextUtils.isEmpty(valor)) {
            contenedor.setError(getString(R.string.error_campo_obligatorio));
            return false;
        }
        contenedor.setError(null);
        return true;
    }

    /** Devuelve Double.NaN si el texto no es un número válido y marca el error. */
    protected double numero(TextInputLayout contenedor, String valor, boolean obligatorio) {
        if (TextUtils.isEmpty(valor)) {
            if (obligatorio) {
                contenedor.setError(getString(R.string.error_campo_obligatorio));
                return Double.NaN;
            }
            contenedor.setError(null);
            return Double.NEGATIVE_INFINITY; // convención: "campo vacío opcional"
        }
        try {
            double d = Double.parseDouble(valor.replace(',', '.'));
            contenedor.setError(null);
            return d;
        } catch (NumberFormatException e) {
            contenedor.setError(getString(R.string.error_numero_invalido));
            return Double.NaN;
        }
    }

    protected boolean esVacioOpcional(double valor) {
        return valor == Double.NEGATIVE_INFINITY;
    }

    // ---------------------- CONFIRMACIÓN ----------------------

    protected void avisarGuardado(String detalleExtra) {
        String cuerpo = getString(R.string.guardado_ok_detalle);
        if (!TextUtils.isEmpty(detalleExtra)) {
            cuerpo = detalleExtra + "\n\n" + cuerpo;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_check)
                .setTitle(R.string.guardado_ok)
                .setMessage(cuerpo)
                .setPositiveButton(R.string.aceptar, null)
                .show();
    }
}
