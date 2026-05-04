package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import com.example.ieperuanosuizoapp.api.ApiClient;
import com.example.ieperuanosuizoapp.api.ApiResponse;
import com.example.ieperuanosuizoapp.models.UsuarioQr;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuariosActivity extends AppCompatActivity {

    private RecyclerView rvUsuarios;
    private UsuariosAdapter adapter;
    private List<UsuarioQr> listaUsuarios = new ArrayList<>();
    private List<UsuarioQr> listaUsuariosFiltrada = new ArrayList<>();
    private View layoutEmptyState;
    private ProgressBar progressLoading;
    private TextInputEditText searchEditText;
    private TextView tvTotalUsuarios;
    private ChipGroup chipGroupFilters;
    private String currentFilter = "Todos";
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        // Verificar autenticación
        if (!ApiClient.isLoggedIn(this)) {
            Intent intent = new Intent(this, AuthLogin.class);
            startActivity(intent);
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupNavigation();
        
        // Cargar usuarios
        loadUsuarios();
    }

    private void initializeViews() {
        rvUsuarios = findViewById(R.id.rv_usuarios);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        progressLoading = findViewById(R.id.progress_loading);
        searchEditText = findViewById(R.id.search_edit_text);
        tvTotalUsuarios = findViewById(R.id.tv_total_usuarios);
        chipGroupFilters = findViewById(R.id.chip_group_filters);
        
        // Botón de retroceso
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });
    }

    private void setupRecyclerView() {
        rvUsuarios.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsuariosAdapter(listaUsuariosFiltrada);
        rvUsuarios.setAdapter(adapter);
        
        // Optimizaciones de rendimiento
        rvUsuarios.setHasFixedSize(true);
        rvUsuarios.setItemViewCacheSize(20);
    }

    private void loadUsuarios() {
        showLoading(true);
        
        String token = ApiClient.getAuthToken(this);
        ApiClient.getApiService().getUsuarios(token).enqueue(new Callback<ApiResponse<List<UsuarioQr>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UsuarioQr>>> call, Response<ApiResponse<List<UsuarioQr>>> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    listaUsuarios = response.body().getData();
                    updateTotalCount();
                    applyCurrentFilter();
                } else {
                    showEmptyState();
                    Toast.makeText(UsuariosActivity.this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UsuarioQr>>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(UsuariosActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void showLoading(boolean show) {
        progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        rvUsuarios.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }
    
    private void showEmptyState() {
        rvUsuarios.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
        progressLoading.setVisibility(View.GONE);
    }

    private void setupSearch() {
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Cancelar búsqueda anterior
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    
                    // Programar nueva búsqueda con delay para evitar lag
                    searchRunnable = () -> applyCurrentFilter();
                    searchHandler.postDelayed(searchRunnable, 300);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupFilters() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = findViewById(checkedIds.get(0));
                currentFilter = selectedChip.getText().toString();
                applyCurrentFilter();
            }
        });
    }

    private void applyCurrentFilter() {
        String searchQuery = searchEditText.getText().toString().toLowerCase().trim();
        
        listaUsuariosFiltrada.clear();
        
        for (UsuarioQr usuario : listaUsuarios) {
            boolean matchesSearch = searchQuery.isEmpty() || 
                usuario.getNombre_completo().toLowerCase().contains(searchQuery) ||
                usuario.getCodigo().toLowerCase().contains(searchQuery) ||
                usuario.getSeccion().toLowerCase().contains(searchQuery) ||
                usuario.getEmail().toLowerCase().contains(searchQuery);
            
            boolean matchesFilter = currentFilter.equals("Todos") || 
                usuario.getSeccion().contains(currentFilter.replace("1ro ", ""));
            
            if (matchesSearch && matchesFilter) {
                listaUsuariosFiltrada.add(usuario);
            }
        }
        
        adapter.notifyDataSetChanged();
        updateTotalCount();
        
        if (listaUsuariosFiltrada.isEmpty()) {
            showEmptyState();
        } else {
            rvUsuarios.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void updateTotalCount() {
        int total = listaUsuariosFiltrada.size();
        tvTotalUsuarios.setText(total + (total == 1 ? " estudiante" : " estudiantes"));
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Definir colores: Rojo para seleccionado, Gris para normal
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{
                Color.parseColor("#BA1924"),
                Color.parseColor("#5E5F60")
        };
        ColorStateList navTint = new ColorStateList(states, colors);

        bottomNav.setItemIconTintList(navTint);
        bottomNav.setSelectedItemId(R.id.nav_more);

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.nav_homework) {
                Intent intent = new Intent(this, CursosActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.nav_more) {
                return true;
            }
            return false;
        });
    }
    
    private void downloadQrImage(UsuarioQr usuario) {
        try {
            // Verificar permisos de almacenamiento
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ no necesita permisos para MediaStore
                saveQrToGallery(usuario);
            } else {
                // Android 9 y anteriores necesitan permisos
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    saveQrToGallery(usuario);
                } else {
                    // Solicitar permisos
                    requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
                    Toast.makeText(this, "Se necesitan permisos de almacenamiento", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al descargar QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQrToGallery(UsuarioQr usuario) {
        new Thread(() -> {
            try {
                // Decodificar la imagen base64
                String base64Image = usuario.getQr_image();
                if (base64Image == null || base64Image.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "No hay imagen QR disponible", Toast.LENGTH_SHORT).show());
                    return;
                }

                if (base64Image.startsWith("data:image/png;base64,")) {
                    base64Image = base64Image.substring("data:image/png;base64,".length());
                }

                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (bitmap == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error al procesar la imagen QR", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Crear nombre de archivo único
                String fileName = "QR_" + usuario.getCodigo() + "_" + 
                    usuario.getNombre_completo().replaceAll("[^a-zA-Z0-9]", "_") + 
                    "_" + System.currentTimeMillis() + ".png";

                // Guardar en galería usando MediaStore
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(android.provider.MediaStore.Images.Media.DESCRIPTION, "Código QR de " + usuario.getNombre_completo());

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, 
                        android.os.Environment.DIRECTORY_PICTURES + "/QR_Codes");
                }

                android.net.Uri uri = getContentResolver().insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (java.io.OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                        
                        runOnUiThread(() -> {
                            Toast.makeText(this, "QR guardado en galería: " + usuario.getNombre_completo(), 
                                Toast.LENGTH_LONG).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Error al guardar en galería", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al guardar QR: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void shareQrImage(UsuarioQr usuario) {
        new Thread(() -> {
            try {
                // Decodificar la imagen base64
                String base64Image = usuario.getQr_image();
                if (base64Image == null || base64Image.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "No hay imagen QR disponible", Toast.LENGTH_SHORT).show());
                    return;
                }

                if (base64Image.startsWith("data:image/png;base64,")) {
                    base64Image = base64Image.substring("data:image/png;base64,".length());
                }

                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (bitmap == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error al procesar la imagen QR", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Guardar temporalmente para compartir
                String fileName = "QR_" + usuario.getCodigo() + "_temp.png";
                java.io.File cachePath = new java.io.File(getCacheDir(), "images");
                cachePath.mkdirs();
                java.io.File file = new java.io.File(cachePath, fileName);

                try (java.io.FileOutputStream stream = new java.io.FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                }

                // Crear URI usando FileProvider
                android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", file);

                runOnUiThread(() -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/png");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, 
                        "Código QR de " + usuario.getNombre_completo() + " - " + usuario.getSeccion());
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Código QR - " + usuario.getNombre_completo());
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(Intent.createChooser(shareIntent, "Compartir QR"));
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al compartir QR: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos concedidos. Intenta descargar nuevamente.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Se necesitan permisos para guardar en galería", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Adapter optimizado para la lista de usuarios
    class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.ViewHolder> {
        private List<UsuarioQr> usuarios;
        
        UsuariosAdapter(List<UsuarioQr> usuarios) { 
            this.usuarios = usuarios; 
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario_qr, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            UsuarioQr usuario = usuarios.get(position);
            
            holder.tvNombreUsuario.setText(usuario.getNombre_completo());
            holder.tvCodigoUsuario.setText(usuario.getCodigo());
            holder.tvSeccionUsuario.setText(usuario.getSeccion());
            holder.tvEmailUsuario.setText(usuario.getEmail());
            
            // Cargar QR de forma asíncrona para evitar lag
            loadQrImageAsync(holder.ivQrCode, usuario.getQr_image());
            
            // Click listeners
            holder.btnDownload.setOnClickListener(v -> downloadQrImage(usuario));
            holder.btnShare.setOnClickListener(v -> shareQrImage(usuario));
        }

        private void loadQrImageAsync(ImageView imageView, String qrImage) {
            if (qrImage != null && !qrImage.isEmpty()) {
                // Cargar en hilo separado para evitar lag
                new Thread(() -> {
                    try {
                        String base64Image = qrImage;
                        if (base64Image.startsWith("data:image/png;base64,")) {
                            base64Image = base64Image.substring("data:image/png;base64,".length());
                        }
                        byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        
                        // Actualizar UI en hilo principal
                        runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                    } catch (Exception e) {
                        runOnUiThread(() -> imageView.setImageResource(R.drawable.ic_qr_placeholder));
                    }
                }).start();
            } else {
                imageView.setImageResource(R.drawable.ic_qr_placeholder);
            }
        }

        @Override
        public int getItemCount() { 
            return usuarios.size(); 
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombreUsuario, tvCodigoUsuario, tvSeccionUsuario, tvEmailUsuario;
            ImageView ivQrCode;
            View btnDownload, btnShare;
            
            ViewHolder(View v) {
                super(v);
                tvNombreUsuario = v.findViewById(R.id.tv_nombre_usuario);
                tvCodigoUsuario = v.findViewById(R.id.tv_codigo_usuario);
                tvSeccionUsuario = v.findViewById(R.id.tv_seccion_usuario);
                tvEmailUsuario = v.findViewById(R.id.tv_email_usuario);
                ivQrCode = v.findViewById(R.id.iv_qr_code);
                btnDownload = v.findViewById(R.id.btn_download_qr);
                btnShare = v.findViewById(R.id.btn_share_qr);
            }
        }
    }
}