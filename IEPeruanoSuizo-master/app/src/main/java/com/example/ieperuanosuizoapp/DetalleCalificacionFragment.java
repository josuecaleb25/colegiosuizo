package com.example.ieperuanosuizoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ieperuanosuizoapp.api.models.Estudiante;
import java.util.ArrayList;
import java.util.List;

public class DetalleCalificacionFragment extends Fragment {

    private String estudianteNombre;
    private TextView tvPromedioTotal;

    public static DetalleCalificacionFragment newInstance(String nombre) {
        DetalleCalificacionFragment fragment = new DetalleCalificacionFragment();
        Bundle args = new Bundle();
        args.putString("nombre", nombre);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            estudianteNombre = getArguments().getString("nombre");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detalle_calificacion_estudiante, container, false);

        TextView tvNombre = view.findViewById(R.id.tv_nombre_estudiante);
        tvNombre.setText(estudianteNombre);
        
        tvPromedioTotal = view.findViewById(R.id.tv_promedio_total);

        // Lógica de navegación con el botón "Atrás"
        view.findViewById(R.id.btn_atras_detalle).setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        RecyclerView rv = view.findViewById(R.id.rv_evaluaciones_estudiante);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        
        EvaluacionesAdapter adapter = new EvaluacionesAdapter();
        rv.setAdapter(adapter);

        // Datos de prueba según tu imagen
        List<String> items = new ArrayList<>();
        items.add("Actitudes");
        items.add("Participacion");
        items.add("Proyecto");
        items.add("Examen I");
        items.add("Examen II");
        items.add("Examen final");
        adapter.setItems(items);

        return view;
    }

    private void mostrarDialogoEditarNota(String evaluacion, String notaActual, int position, EvaluacionesAdapter adapter) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_editar_nota, null);
        
        TextView tvSubtitle = v.findViewById(R.id.tv_dialog_subtitle);
        android.widget.EditText etNota = v.findViewById(R.id.et_nota);
        
        tvSubtitle.setText("Evaluación: " + evaluacion);
        if (!notaActual.equals("--")) {
            etNota.setText(notaActual);
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(v)
                .create();

        v.findViewById(R.id.btn_cancelar).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_guardar).setOnClickListener(view -> {
            String input = etNota.getText().toString().replace(",", ".");
            if (input.isEmpty()) {
                adapter.actualizarNota(position, "--");
                dialog.dismiss();
                return;
            }

            try {
                double valor = Double.parseDouble(input);
                if (valor < 0 || valor > 20) {
                    etNota.setError("Rango 0-20");
                } else {
                    // Formatear a 1 decimal
                    String notaFormateada = String.format(java.util.Locale.US, "%.1f", valor);
                    adapter.actualizarNota(position, notaFormateada);
                    dialog.dismiss();
                }
            } catch (NumberFormatException e) {
                etNota.setError("Nota inválida");
            }
        });

        dialog.show();
        configurarAnchoDialogo(dialog);
    }

    private void configurarAnchoDialogo(android.app.AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private void calcularYMostrarPromedio(java.util.Map<Integer, String> notas, int totalItems) {
        if (totalItems == 0) {
            tvPromedioTotal.setText("--");
            return;
        }

        double suma = 0;
        int contadas = 0;

        for (String n : notas.values()) {
            if (n != null && !n.equals("--")) {
                try {
                    suma += Double.parseDouble(n);
                    contadas++;
                } catch (NumberFormatException ignored) {}
            }
        }

        if (contadas == 0) {
            tvPromedioTotal.setText("--");
        } else {
            double promedio = suma / contadas; // Promedio solo sobre las notas que ya han sido ingresadas
            tvPromedioTotal.setText(String.format(java.util.Locale.US, "%.1f", promedio));
        }
    }

    private class EvaluacionesAdapter extends RecyclerView.Adapter<EvaluacionesAdapter.ViewHolder> {
        private List<String> items = new ArrayList<>();
        private java.util.Map<Integer, String> notas = new java.util.HashMap<>();

        void setItems(List<String> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        void actualizarNota(int position, String nuevaNota) {
            notas.put(position, nuevaNota);
            notifyItemChanged(position);
            calcularYMostrarPromedio(notas, items.size());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calificacion_fila, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item = items.get(position);
            String nota = notas.getOrDefault(position, "--");
            
            holder.tvNombre.setText(item);
            holder.tvNota.setText(nota);

            holder.itemView.setOnClickListener(v -> {
                mostrarDialogoEditarNota(item, nota, position, this);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvNota;
            ViewHolder(View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tv_nombre_evaluacion);
                tvNota = itemView.findViewById(R.id.tv_nota);
            }
        }
    }
}
