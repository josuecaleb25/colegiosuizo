package com.example.ieperuanosuizoapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CambiarContrasenaActivity extends AppCompatActivity {
    private TextInputEditText currentPasswordInput;
    private TextInputEditText newPasswordInput;
    private TextInputEditText confirmPasswordInput;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_contrasena);

        currentPasswordInput = findViewById(R.id.current_password);
        newPasswordInput = findViewById(R.id.new_password);
        confirmPasswordInput = findViewById(R.id.confirm_new_password);
        saveButton = findViewById(R.id.save_password);
        saveButton.setOnClickListener(view -> savePassword());
        findViewById(R.id.back_change_password).setOnClickListener(view -> finish());
    }

    private void savePassword() {
        String currentPassword = rawValue(currentPasswordInput);
        String newPassword = rawValue(newPasswordInput);
        String confirmation = rawValue(confirmPasswordInput);

        if (currentPassword.isEmpty() || newPassword.length() < 12 || !newPassword.equals(confirmation)) {
            Toast.makeText(this, "Ingresa la contraseña actual y una nueva de al menos 12 caracteres.", Toast.LENGTH_LONG).show();
            return;
        }

        saveButton.setEnabled(false);
        Map<String, String> body = new HashMap<>();
        body.put("current_password", currentPassword);
        body.put("new_password", newPassword);

        RetrofitClient.getApiService().changePassword(body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                saveButton.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CambiarContrasenaActivity.this, "Contraseña actualizada.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                Toast.makeText(CambiarContrasenaActivity.this, "No se pudo cambiar. Verifica tu contraseña actual.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable error) {
                saveButton.setEnabled(true);
                Toast.makeText(CambiarContrasenaActivity.this, "No se pudo conectar con el servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String rawValue(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
