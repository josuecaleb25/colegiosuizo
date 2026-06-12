package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.example.ieperuanosuizoapp.api.RetrofitClient;

public class IdentificacionActivity extends AppCompatActivity {

    private TextView tvFullName, tvCodigo, tvSeccion, tvEmail;
    private ImageView ivQr;
    private ProgressBar progressQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        int colorScheme = themePrefs.getInt("colorScheme", 0);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        if (colorScheme == 2) {
            setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identificacion);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvFullName = findViewById(R.id.tv_full_name);
        tvCodigo = findViewById(R.id.tv_codigo);
        tvSeccion = findViewById(R.id.tv_seccion2);
        tvEmail = findViewById(R.id.tv_email);
        ivQr = findViewById(R.id.iv_qr);
        progressQr = findViewById(R.id.progress_qr);

        findViewById(R.id.btn_compartir).setOnClickListener(v -> compartirIdentificacion());

        ivQr.setImageResource(R.drawable.logoie);
        llenarDatosLocales();
        cargarPerfil();
    }

    private void llenarDatosLocales() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String nombreCompleto = prefs.getString("user_name", "Usuario");
        String seccion = prefs.getString("user_seccion", "");
        String email = prefs.getString("user_email", "");
        String codigo = prefs.getString("user_codigo", "");

        tvFullName.setText(nombreCompleto.toUpperCase());
        if (!seccion.isEmpty()) {
            tvSeccion.setText(seccion);
        }
        if (!email.isEmpty()) {
            tvEmail.setText(email);
        }
        if (!codigo.isEmpty()) {
            StringBuilder spacedId = new StringBuilder();
            for (char c : codigo.toCharArray()) {
                spacedId.append(c).append(" ");
            }
            tvCodigo.setText(spacedId.toString().trim());
        }
    }

    private void cargarPerfil() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");

        if (!userId.isEmpty()) {
            progressQr.setVisibility(View.VISIBLE);
            RetrofitClient.getApiService().getPerfilUsuario(userId)
                .enqueue(new retrofit2.Callback<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call,
                                         retrofit2.Response<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> response) {
                        progressQr.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            parsearPerfil(response.body().getData());
                        } else {
                            int code = response.code();
                            String msg = response.body() != null ? response.body().getMessage() : "null";
                            android.util.Log.w("Identificacion", "Perfil API error " + code + ": " + msg);
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.ieperuanosuizoapp.api.models.ApiResponse<Object>> call, Throwable t) {
                        progressQr.setVisibility(View.GONE);
                        ivQr.setImageResource(R.drawable.logoie);
                        android.util.Log.e("Identificacion", "Perfil API onFailure", t);
                        // Fallback: mostrar últimos dígitos del ID como código
                        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        String userId = prefs.getString("user_id", "");
                        if (userId.length() >= 4) {
                            String fallbackCodigo = userId.substring(userId.length() - 4);
                            StringBuilder spacedId = new StringBuilder();
                            for (char c : fallbackCodigo.toCharArray()) {
                                spacedId.append(c).append(" ");
                            }
                            tvCodigo.setText(spacedId.toString().trim());
                            prefs.edit().putString("user_codigo", fallbackCodigo).apply();
                        }
                    }
                });
        }
    }

    private void parsearPerfil(Object data) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonObject json = gson.toJsonTree(data).getAsJsonObject();

            SharedPreferences.Editor editor = getSharedPreferences("user_prefs", MODE_PRIVATE).edit();

            if (json.has("persona")) {
                com.google.gson.JsonObject persona = json.getAsJsonObject("persona");
                String nombres = persona.has("nombres") ? persona.get("nombres").getAsString() : "";
                String apellidos = persona.has("apellidos") ? persona.get("apellidos").getAsString() : "";

                tvFullName.setText((nombres + " " + apellidos).toUpperCase());
            }

            if (json.has("email")) {
                String email = json.get("email").getAsString();
                tvEmail.setText(email);
                editor.putString("user_email", email);
            }

            if (json.has("alumno")) {
                com.google.gson.JsonObject alumno = json.getAsJsonObject("alumno");
                String codigo = alumno.has("codigo") && !alumno.get("codigo").isJsonNull()
                    ? alumno.get("codigo").getAsString() : "";
                String seccion = alumno.has("seccion") && !alumno.get("seccion").isJsonNull()
                    ? alumno.get("seccion").getAsString() : "";
                String qrImage = alumno.has("qr_image") && !alumno.get("qr_image").isJsonNull()
                    ? alumno.get("qr_image").getAsString() : "";

                if (!codigo.isEmpty()) {
                    StringBuilder spacedId = new StringBuilder();
                    for (char c : codigo.toCharArray()) {
                        spacedId.append(c).append(" ");
                    }
                    tvCodigo.setText(spacedId.toString().trim());
                    editor.putString("user_codigo", codigo);
                }
                if (!seccion.isEmpty()) {
                    tvSeccion.setText(seccion);
                    editor.putString("user_seccion", seccion);
                }
                if (!qrImage.isEmpty()) {
                    cargarQRDesdeBase64(qrImage);
                }
            }

            editor.apply();
        } catch (Exception e) {
            android.util.Log.e("Identificacion", "Error parseando perfil", e);
        }
    }

    private void cargarQRDesdeBase64(String qrDataUrl) {
        try {
            String base64 = qrDataUrl;
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap qrBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (qrBitmap != null) {
                ivQr.setImageBitmap(qrBitmap);
                return;
            }
        } catch (Exception e) {
            android.util.Log.e("Identificacion", "Error decodificando QR", e);
        }
        ivQr.setImageResource(R.drawable.logoie);
    }

    private void compartirIdentificacion() {
        CardView card = findViewById(R.id.card_identificacion);
        card.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(card.getDrawingCache());
        card.setDrawingCacheEnabled(false);

        try {
            java.io.File cachePath = new java.io.File(getCacheDir(), "images");
            cachePath.mkdirs();
            java.io.File imageFile = new java.io.File(cachePath, "identificacion_temp.png");
            java.io.FileOutputStream stream = new java.io.FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".fileprovider", imageFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Compartir Identificación"));
        } catch (Exception e) {
            Toast.makeText(this, "Error al compartir", Toast.LENGTH_SHORT).show();
        }
    }
}
