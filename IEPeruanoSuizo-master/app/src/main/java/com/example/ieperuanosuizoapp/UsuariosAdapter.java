package com.example.ieperuanosuizoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ieperuanosuizoapp.api.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder> {

    private List<Usuario> usuarios;
    private OnUsuarioClickListener listener;

    public interface OnUsuarioClickListener {
        void onUsuarioClick(Usuario usuario);
    }

    public UsuariosAdapter(OnUsuarioClickListener listener) {
        this.usuarios = new ArrayList<>();
        this.listener = listener;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios != null ? usuarios : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usuario, parent, false);
        return new UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        holder.bind(usuario, listener);
    }

    @Override
    public int getItemCount() {
        return usuarios.size();
    }

    static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivQrCode;
        private TextView tvNombreCompleto;
        private TextView tvSeccion;
        private TextView tvCodigo;
        private TextView tvEstado;
        private TextView tvEmail;
        private View btnDownload;
        private View btnShare;

        public UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivQrCode = itemView.findViewById(R.id.iv_qr_code);
            tvNombreCompleto = itemView.findViewById(R.id.tv_nombre_completo);
            tvSeccion = itemView.findViewById(R.id.tv_seccion);
            tvCodigo = itemView.findViewById(R.id.tv_codigo);
            tvEstado = itemView.findViewById(R.id.tv_estado);
            tvEmail = itemView.findViewById(R.id.tv_email);
            btnDownload = itemView.findViewById(R.id.btn_download_qr);
            btnShare = itemView.findViewById(R.id.btn_share_qr);
        }

        public void bind(Usuario usuario, OnUsuarioClickListener listener) {
            tvNombreCompleto.setText(usuario.getNombreCompleto());
            tvSeccion.setText(usuario.getSeccion());
            tvCodigo.setText(usuario.getCodigoAlumno());
            tvEmail.setText(usuario.getEmail() != null ? usuario.getEmail() : "Sin email");
            
            // Estado
            String estado = usuario.getEstado();
            tvEstado.setText(estado.substring(0, 1).toUpperCase() + estado.substring(1));
            
            if ("activo".equalsIgnoreCase(estado)) {
                tvEstado.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
            } else {
                tvEstado.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
            }

            // Cargar QR code desde base64
            if (usuario.getQrImage() != null && !usuario.getQrImage().isEmpty()) {
                try {
                    String base64Image = usuario.getQrImage();
                    // Remover el prefijo data:image/png;base64, si existe
                    if (base64Image.startsWith("data:image")) {
                        base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                    }
                    byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        ivQrCode.setImageBitmap(bitmap);
                        ivQrCode.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                    } else {
                        ivQrCode.setImageResource(R.drawable.ic_qr_placeholder);
                    }
                } catch (Exception e) {
                    android.util.Log.e("UsuariosAdapter", "Error decodificando QR: " + e.getMessage());
                    ivQrCode.setImageResource(R.drawable.ic_qr_placeholder);
                }
            } else {
                android.util.Log.w("UsuariosAdapter", "QR image vacío para: " + usuario.getNombreCompleto());
                ivQrCode.setImageResource(R.drawable.ic_qr_placeholder);
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUsuarioClick(usuario);
                }
            });

            btnDownload.setOnClickListener(v -> {
                android.widget.Toast.makeText(itemView.getContext(), 
                    "Descargar QR de " + usuario.getNombreCompleto(), 
                    android.widget.Toast.LENGTH_SHORT).show();
                // TODO: Implementar descarga de QR
            });

            btnShare.setOnClickListener(v -> {
                android.widget.Toast.makeText(itemView.getContext(), 
                    "Compartir QR de " + usuario.getNombreCompleto(), 
                    android.widget.Toast.LENGTH_SHORT).show();
                // TODO: Implementar compartir QR
            });
        }
    }
}
