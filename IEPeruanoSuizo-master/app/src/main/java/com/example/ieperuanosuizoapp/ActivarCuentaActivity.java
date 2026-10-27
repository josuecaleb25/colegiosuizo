package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivarCuentaActivity extends AppCompatActivity {
    private TextInputEditText emailInput;
    private TextInputEditText codeInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private Button activateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activar_cuenta);

        emailInput = findViewById(R.id.activation_email);
        codeInput = findViewById(R.id.activation_code);
        passwordInput = findViewById(R.id.activation_password);
        confirmPasswordInput = findViewById(R.id.activation_confirm_password);
        activateButton = findViewById(R.id.activation_submit);

        String email = getIntent().getStringExtra("email");
        if (email != null) emailInput.setText(email);

        activateButton.setOnClickListener(view -> activateAccount());
    }

    private void activateAccount() {
        String email = value(emailInput).toLowerCase();
        String code = value(codeInput);
        String password = rawValue(passwordInput);
        String confirmation = rawValue(confirmPasswordInput);

        if (email.isEmpty() || code.isEmpty() || password.length() < 12 || !password.equals(confirmation)) {
            Toast.makeText(this, "Revisa el correo, el código y las contraseñas. Usa al menos 12 caracteres.", Toast.LENGTH_LONG).show();
            return;
        }

        activateButton.setEnabled(false);
        activateButton.setText("Activando cuenta...");

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("activation_code", code);
        body.put("password", password);

        RetrofitClient.getApiService().activateAccount(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                activateButton.setEnabled(true);
                activateButton.setText("Activar cuenta");

                if (response.isSuccessful()) {
                    Toast.makeText(ActivarCuentaActivity.this, "Cuenta activada. Inicia sesión con tu nueva contraseña.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ActivarCuentaActivity.this, AuthLogin.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                    return;
                }

                if (response.errorBody() != null) response.errorBody().close();
                Toast.makeText(ActivarCuentaActivity.this, "No se pudo activar la cuenta. Verifica el código o solicita uno nuevo.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable error) {
                activateButton.setEnabled(true);
                activateButton.setText("Activar cuenta");
                Toast.makeText(ActivarCuentaActivity.this, "No se pudo conectar con el servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String value(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private String rawValue(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
