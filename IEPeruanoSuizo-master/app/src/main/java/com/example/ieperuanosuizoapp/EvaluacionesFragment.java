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
import java.util.ArrayList;
import java.util.List;

public class EvaluacionesFragment extends Fragment {

    public static EvaluacionesFragment newInstance() {
        return new EvaluacionesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_evaluaciones_profesor, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_evaluaciones);
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

        view.findViewById(R.id.btn_add_evaluacion).setOnClickListener(v -> {
            mostrarDialogoAgregar();
        });

        return view;
    }

    private void mostrarDialogoAgregar() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_nueva_evaluacion, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(v)
                .create();

        v.findViewById(R.id.btn_cancelar).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_aceptar).setOnClickListener(view -> {
            String nombre = ((android.widget.EditText) v.findViewById(R.id.et_nombre_evaluacion)).getText().toString();
            if (!nombre.isEmpty()) {
                // Aquí iría la lógica para guardar en el backend
                android.widget.Toast.makeText(getContext(), "Evaluación creada: " + nombre, android.widget.Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.show();
        configurarAnchoDialogo(dialog);
    }

    private void mostrarDialogoEditar(String nombreActual) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_nueva_evaluacion, null);
        
        TextView tvTitle = v.findViewById(R.id.tv_dialog_title);
        TextView tvSubtitle = v.findViewById(R.id.tv_dialog_subtitle);
        android.widget.EditText etNombre = v.findViewById(R.id.et_nombre_evaluacion);
        
        tvTitle.setText("Editar Evaluacion");
        tvSubtitle.setText("Nuevo nombre de " + nombreActual);
        etNombre.setText(nombreActual);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(v)
                .create();

        v.findViewById(R.id.btn_cancelar).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_aceptar).setOnClickListener(view -> {
            String nuevoNombre = etNombre.getText().toString();
            if (!nuevoNombre.isEmpty()) {
                android.widget.Toast.makeText(getContext(), "Evaluación actualizada", android.widget.Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.show();
        configurarAnchoDialogo(dialog);
    }

    private void mostrarDialogoEliminar() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_eliminar_evaluacion, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(v)
                .create();

        v.findViewById(R.id.btn_cancelar).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_aceptar).setOnClickListener(view -> {
            android.widget.Toast.makeText(getContext(), "Evaluación eliminada", android.widget.Toast.LENGTH_SHORT).show();
            dialog.dismiss();
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

    private class EvaluacionesAdapter extends RecyclerView.Adapter<EvaluacionesAdapter.ViewHolder> {
        private List<String> items = new ArrayList<>();

        void setItems(List<String> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_evaluacion_profesor, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item = items.get(position);
            holder.tvNombre.setText(item);
            
            holder.btnEdit.setOnClickListener(v -> {
                mostrarDialogoEditar(item);
            });
            
            holder.btnDelete.setOnClickListener(v -> {
                mostrarDialogoEliminar();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre;
            View btnEdit, btnDelete;
            ViewHolder(View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tv_nombre_eval);
                btnEdit = itemView.findViewById(R.id.btn_edit_eval);
                btnDelete = itemView.findViewById(R.id.btn_delete_eval);
            }
        }
    }
}
