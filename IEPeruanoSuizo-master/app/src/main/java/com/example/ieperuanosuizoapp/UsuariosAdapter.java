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
                descargarQR(usuario);
            });

            btnShare.setOnClickListener(v -> {
                compartirQR(usuario);
            });
        }
        
        private void descargarQR(Usuario usuario) {
            try {
                // Obtener el bitmap del QR
                android.graphics.Bitmap qrBitmap = obtenerBitmapQR(usuario);
                if (qrBitmap == null) {
                    android.widget.Toast.makeText(itemView.getContext(), 
                        "No se pudo obtener el código QR", 
                        android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Guardar en la galería
                String nombreArchivo = "QR_" + usuario.getCodigoAlumno() + "_" + System.currentTimeMillis() + ".png";
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Android 10+ usar MediaStore
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, nombreArchivo);
                    values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
                    values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/IEPS_QR");
                    
                    android.content.ContentResolver resolver = itemView.getContext().getContentResolver();
                    android.net.Uri imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    
                    if (imageUri != null) {
                        java.io.OutputStream outputStream = resolver.openOutputStream(imageUri);
                        qrBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream);
                        outputStream.close();
                        
                        android.widget.Toast.makeText(itemView.getContext(), 
                            "QR guardado en Galería/IEPS_QR", 
                            android.widget.Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Android 9 y anteriores
                    java.io.File picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES);
                    java.io.File iepsDir = new java.io.File(picturesDir, "IEPS_QR");
                    if (!iepsDir.exists()) {
                        iepsDir.mkdirs();
                    }
                    
                    java.io.File imageFile = new java.io.File(iepsDir, nombreArchivo);
                    java.io.FileOutputStream outputStream = new java.io.FileOutputStream(imageFile);
                    qrBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();
                    
                    // Notificar a la galería
                    android.content.Intent mediaScanIntent = new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(android.net.Uri.fromFile(imageFile));
                    itemView.getContext().sendBroadcast(mediaScanIntent);
                    
                    android.widget.Toast.makeText(itemView.getContext(), 
                        "QR guardado en Galería/IEPS_QR", 
                        android.widget.Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                android.util.Log.e("UsuariosAdapter", "Error guardando QR: " + e.getMessage());
                android.widget.Toast.makeText(itemView.getContext(), 
                    "Error al guardar QR: " + e.getMessage(), 
                    android.widget.Toast.LENGTH_SHORT).show();
            }
        }
        
        private void compartirQR(Usuario usuario) {
            try {
                // Obtener el bitmap del QR
                android.graphics.Bitmap qrBitmap = obtenerBitmapQR(usuario);
                if (qrBitmap == null) {
                    android.widget.Toast.makeText(itemView.getContext(), 
                        "No se pudo obtener el código QR", 
                        android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Guardar temporalmente en caché
                java.io.File cachePath = new java.io.File(itemView.getContext().getCacheDir(), "images");
                cachePath.mkdirs();
                java.io.File imageFile = new java.io.File(cachePath, "qr_temp.png");
                java.io.FileOutputStream stream = new java.io.FileOutputStream(imageFile);
                qrBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
                stream.close();
                
                // Crear URI usando FileProvider
                android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    itemView.getContext(),
                    itemView.getContext().getPackageName() + ".fileprovider",
                    imageFile);
                
                // Compartir
                android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, 
                    "Código QR de " + usuario.getNombreCompleto() + " - " + usuario.getCodigoAlumno());
                shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                itemView.getContext().startActivity(android.content.Intent.createChooser(shareIntent, "Compartir QR"));
                
            } catch (Exception e) {
                android.util.Log.e("UsuariosAdapter", "Error compartiendo QR: " + e.getMessage());
                android.widget.Toast.makeText(itemView.getContext(), 
                    "Error al compartir QR: " + e.getMessage(), 
                    android.widget.Toast.LENGTH_SHORT).show();
            }
        }
        
        private android.graphics.Bitmap obtenerBitmapQR(Usuario usuario) {
            if (usuario.getQrImage() == null || usuario.getQrImage().isEmpty()) {
                return null;
            }
            
            try {
                String base64Image = usuario.getQrImage();
                // Remover el prefijo data:image/png;base64, si existe
                if (base64Image.startsWith("data:image")) {
                    base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                }
                byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                return android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            } catch (Exception e) {
                android.util.Log.e("UsuariosAdapter", "Error obteniendo bitmap QR: " + e.getMessage());
                return null;
            }
        }
    }
}
