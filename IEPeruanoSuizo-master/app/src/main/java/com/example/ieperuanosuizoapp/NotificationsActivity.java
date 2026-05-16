package com.example.ieperuanosuizoapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Cargar tema
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

        RecyclerView rv = findViewById(R.id.rv_notifications);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // Datos de ejemplo para mostrar el diseño estilo Instagram
        List<NotifItem> list = new ArrayList<>();
        list.add(new NotifItem("Hoy", true));
        list.add(new NotifItem("La Dirección publicó un nuevo comunicado: 'Protocolo de Invierno'. 2h", false));
        list.add(new NotifItem("Prof. Ricardo Huaman subió material al curso de Computación. 5h", false));
        
        list.add(new NotifItem("Ayer", true));
        list.add(new NotifItem("Tu asistencia del martes 22 ha sido registrada como 'A tiempo'. 1d", false));
        list.add(new NotifItem("Nuevo comunicado de Salón: 'Reunión de Padres de Familia'. 1d", false));
        
        list.add(new NotifItem("Últimos 7 días", true));
        list.add(new NotifItem("Se ha actualizado el horario de clases para el mes de Mayo. 4d", false));
        list.add(new NotifItem("Bienvenido a la nueva App del Colegio Peruano Suizo. 1sem", false));

        rv.setAdapter(new NotifAdapter(list));
    }

    private static class NotifItem {
        String text;
        boolean isHeader;
        NotifItem(String t, boolean h) { text = t; isHeader = h; }
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
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).tv.setText(items.get(position).text);
            } else {
                ((NotifVH) holder).tv.setText(items.get(position).text);
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
