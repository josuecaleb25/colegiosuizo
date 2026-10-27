package com.example.ieperuanosuizoapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ActivationCodeResponse;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivarCuentaActivity extends AppCompatActivity {
    private TextInputEditText emailInput;
    private Button issueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_activar_cuenta);

        emailInput = findViewById(R.id.admin_activation_email);
        issueButton = findViewById(R.id.admin_activation_issue);
        issueButton.setOnClickListener(view -> issueCode());
    }

    private void issueCode() {
        String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim().toLowerCase();
        if (email.isEmpty()) {
            Toast.makeText(this, "Ingresa el correo institucional.", Toast.LENGTH_SHORT).show();
            return;
        }

        issueButton.setEnabled(false);
        issueButton.setText("Generando código...");

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        RetrofitClient.getApiService().issueActivationCode(body).enqueue(new Callback<ActivationCodeResponse>() {
            @Override
            public void onResponse(Call<ActivationCodeResponse> call, Response<ActivationCodeResponse> response) {
                issueButton.setEnabled(true);
                issueButton.setText("Generar código individual");

                ActivationCodeResponse result = response.body();
                if (!response.isSuccessful() || result == null || !result.isSuccess() || result.getData() == null) {
                    if (response.errorBody() != null) response.errorBody().close();
                    Toast.makeText(AdminActivarCuentaActivity.this, "No se pudo generar el código. Verifica el correo y el rol.", Toast.LENGTH_LONG).show();
                    return;
                }

                showCode(result.getData());
            }

            @Override
            public void onFailure(Call<ActivationCodeResponse> call, Throwable error) {
                issueButton.setEnabled(true);
                issueButton.setText("Generar código individual");
                Toast.makeText(AdminActivarCuentaActivity.this, "No se pudo conectar con el servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showCode(ActivationCodeResponse.Data data) {
        String message = "Cuenta: " + data.getEmail() + "\nRol: " + data.getRol() + "\nVence: " + data.getExpiresAt() + "\n\nCódigo de un solo uso:\n" + data.getActivationCode();
        new AlertDialog.Builder(this)
                .setTitle("Código generado una sola vez")
                .setMessage(message)
                .setPositiveButton("Copiar", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("Código de activación", data.getActivationCode()));
                    Toast.makeText(this, "Código copiado. Entrégalo en privado.", Toast.LENGTH_LONG).show();
                })
                .setNeutralButton("Compartir", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, "Código de activación para " + data.getEmail() + ": " + data.getActivationCode());
                    startActivity(Intent.createChooser(intent, "Entregar código en privado"));
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }
}
