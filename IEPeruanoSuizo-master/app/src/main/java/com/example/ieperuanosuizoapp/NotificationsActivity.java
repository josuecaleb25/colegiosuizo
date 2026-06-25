package com.example.ieperuanosuizoapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ieperuanosuizoapp.api.ApiConfig;
import com.example.ieperuanosuizoapp.api.ApiService;
import com.example.ieperuanosuizoapp.api.models.Notificacion;
import com.example.ieperuanosuizoapp.api.models.NotificacionesResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rv;
    private NotifAdapter adapter;
    private List<NotifItem> items = new ArrayList<>();
    private View layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        int colorScheme = sharedPreferences.getInt("colorScheme", 0);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        if (colorScheme == 2) {
            setTheme(R.style.Theme_IEPeruanoSuizoAPP_Green);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_notifications);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotifAdapter(items);
        rv.setAdapter(adapter);

        layoutEmpty = findViewById(R.id.layout_empty_notif);

        cargarNotificaciones();
    }

    private void cargarNotificaciones() {
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String estudianteId = userPrefs.getString("estudiante_id", null);

        if (estudianteId == null) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        ApiService api = retrofit.create(ApiService.class);
        api.getNotificaciones(estudianteId, 1, 50).enqueue(new Callback<NotificacionesResponse>() {
            @Override
            public void onResponse(Call<NotificacionesResponse> call, Response<NotificacionesResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Notificacion> notificaciones = response.body().getData();
                    items.clear();
                    if (notificaciones != null && !notificaciones.isEmpty()) {
                        rv.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                        String currentGroup = "";
                        SimpleDateFormat todayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        String today = todayFmt.format(new Date());

                        for (Notificacion n : notificaciones) {
                            String fechaStr = n.getFechaEnvio();
                            if (fechaStr != null && fechaStr.length() >= 10) {
                                String fechaDay = fechaStr.substring(0, 10);
                                if (!fechaDay.equals(currentGroup)) {
                                    currentGroup = fechaDay;
                                    String label = fechaDay.equals(today) ? "Hoy" : formatearFecha(fechaStr);
                                    items.add(new NotifItem(label, true, null));
                                }
                            }
                            String text = n.getTitulo() + ". " + n.getMensaje();
                            items.add(new NotifItem(text, false, n));
                        }
                    } else {
                        rv.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<NotificacionesResponse> call, Throwable t) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            }
        });
    }

    private String formatearFecha(String fechaIso) {
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date d = iso.parse(fechaIso);
            SimpleDateFormat fmt = new SimpleDateFormat("d 'de' MMMM", new Locale("es", "PE"));
            return fmt.format(d);
        } catch (Exception e) {
            return fechaIso.length() >= 10 ? fechaIso.substring(0, 10) : fechaIso;
        }
    }

    private static class NotifItem {
        String text;
        boolean isHeader;
        Notificacion notificacion;
        NotifItem(String t, boolean h, Notificacion n) { text = t; isHeader = h; notificacion = n; }
    }

    private class NotifAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<NotifItem> items;
        NotifAdapter(List<NotifItem> items) { this.items = items; }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isHeader ? 0 : 1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_header, parent, false);
                return new HeaderVH(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
                return new NotifVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            NotifItem item = items.get(position);
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).tv.setText(item.text);
            } else {
                ((NotifVH) holder).tv.setText(item.text);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class HeaderVH extends RecyclerView.ViewHolder {
            TextView tv;
            HeaderVH(View v) { super(v); tv = v.findViewById(R.id.tv_header_title); }
        }

        class NotifVH extends RecyclerView.ViewHolder {
            TextView tv;
            NotifVH(View v) { super(v); tv = v.findViewById(R.id.tv_notif_content); }
        }
    }
}
