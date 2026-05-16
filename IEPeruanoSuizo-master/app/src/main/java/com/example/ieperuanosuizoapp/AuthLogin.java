package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.LoginRequest;
import com.example.ieperuanosuizoapp.api.models.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthLogin extends AppCompatActivity {

    private EditText etEmail, etPassword;
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

        // Inicializar vistas
        etEmail = findViewById(R.id.inputTypeIDUSER);
        etPassword = findViewById(R.id.inputTypePassword);
        btnLogin = findViewById(R.id.Login);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Deshabilitar botón mientras se procesa
            btnLogin.setEnabled(false);
            btnLogin.setText("Iniciando sesión...");

            // Llamar al backend
            loginUser(email, password);
        });
    }

    private void loginUser(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);

        RetrofitClient.getApiService().login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Iniciar Sesión");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess() && loginResponse.getData() != null) {
                        LoginResponse.Data data = loginResponse.getData();
                        LoginResponse.User user = data.getUser();
                        
                        if (user == null) {
                            Toast.makeText(AuthLogin.this, "Error: datos de usuario incompletos", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Guardar datos del usuario en SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("user_id", user.getId() != null ? user.getId() : "");
                        editor.putString("user_email", user.getEmail() != null ? user.getEmail() : "");
                        editor.putString("user_mode", user.getRol() != null ? user.getRol().toUpperCase() : "ALUMNO");
                        editor.putString("user_token", data.getToken() != null ? data.getToken() : "");
                        editor.putString("user_name", user.getNombreCompleto() != null ? user.getNombreCompleto() : "Usuario");
                        editor.putString("user_seccion", user.getSeccion() != null ? user.getSeccion() : "");
                        editor.apply();

                        // Log para debug
                        android.util.Log.d("AuthLogin", "Usuario ID guardado: " + user.getId());
                        android.util.Log.d("AuthLogin", "Usuario Email: " + user.getEmail());
                        android.util.Log.d("AuthLogin", "Usuario Rol: " + user.getRol());
                        android.util.Log.d("AuthLogin", "Usuario Sección: " + user.getSeccion());

                        String nombreBienvenida = user.getNombres() != null ? user.getNombres() : "Usuario";
                        Toast.makeText(AuthLogin.this, "Bienvenido " + nombreBienvenida, Toast.LENGTH_SHORT).show();

                        // Ir a HomeActivity
                        Intent intent = new Intent(AuthLogin.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        String mensaje = loginResponse.getMessage() != null ? loginResponse.getMessage() : "Error al iniciar sesión";
                        Toast.makeText(AuthLogin.this, mensaje, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AuthLogin.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Iniciar Sesión");
                Toast.makeText(AuthLogin.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}