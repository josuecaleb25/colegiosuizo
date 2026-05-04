package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import com.example.ieperuanosuizoapp.api.ApiClient;
import com.example.ieperuanosuizoapp.api.ApiResponse;
import com.example.ieperuanosuizoapp.models.LoginRequest;
import com.example.ieperuanosuizoapp.models.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthLogin extends AppCompatActivity {
    
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Verificar si ya está logueado
        if (ApiClient.isLoggedIn(this)) {
            goToHome();
            return;
        }

        // Inicializar vistas
        etEmail = findViewById(R.id.inputTypeIDUSER); // Campo de email
        etPassword = findViewById(R.id.inputTypePassword); // Campo de contraseña
        btnLogin = findViewById(R.id.Login);

        btnLogin.setOnClickListener(v -> performLogin());
    }
    
    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando sesión...");
        
        LoginRequest request = new LoginRequest(email, password);
        
        ApiClient.getApiService().login(request).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Iniciar Sesión");
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LoginResponse> apiResponse = response.body();
                    
                    if (apiResponse.isSuccess()) {
                        // Guardar tokens
                        LoginResponse loginData = apiResponse.getData();
                        ApiClient.saveAuthToken(
                            AuthLogin.this,
                            loginData.getTokens().getAccess(),
                            loginData.getTokens().getRefresh()
                        );
                        
                        Toast.makeText(AuthLogin.this, "Login exitoso", Toast.LENGTH_SHORT).show();
                        goToHome();
                    } else {
                        Toast.makeText(AuthLogin.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AuthLogin.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Iniciar Sesión");
                Toast.makeText(AuthLogin.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void goToHome() {
        Intent intent = new Intent(AuthLogin.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}