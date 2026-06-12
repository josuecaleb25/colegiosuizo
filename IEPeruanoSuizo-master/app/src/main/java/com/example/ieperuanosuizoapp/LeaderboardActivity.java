package com.example.ieperuanosuizoapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.LeaderboardEntry;
import com.example.ieperuanosuizoapp.api.models.Section;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private Spinner spinnerSection;
    private RecyclerView rvLeaderboard;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private String userId;
    private String userRol;
    private String currentTipo = "puntual";
    private String currentMes;
    private int currentSeccionId = -1;

    private List<Section> sections = new ArrayList<>();
    private List<LeaderboardEntry> entries = new ArrayList<>();
    private LeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        currentMes = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", "");
        userRol = prefs.getString("user_mode", "ALUMNO").toLowerCase();

        initViews();
        setupBackButton();
        setupTabLayout();
        setupSwipeRefresh();
        setupRecyclerView();
        fetchSections();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout);
        spinnerSection = findViewById(R.id.spinner_section);
        rvLeaderboard = findViewById(R.id.rv_leaderboard);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
        swipeRefresh = findViewById(R.id.swipe_refresh);
    }

    private void setupBackButton() {
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTipo = tab.getPosition() == 0 ? "puntual" : "asistencia";
                adapter.setTipo(currentTipo);
                if (currentSeccionId != -1) {
                    fetchLeaderboard();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            if (currentSeccionId != -1) {
                fetchLeaderboard();
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });
        swipeRefresh.setColorSchemeResources(android.R.color.holo_red_dark);
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter(entries);
        rvLeaderboard.setAdapter(adapter);
        rvLeaderboard.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
    }

    private void fetchSections() {
        showLoading(true);
        RetrofitClient.getApiService().getSeccionesTyped(userId, userRol).enqueue(new Callback<ApiResponse<List<Section>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Section>>> call, Response<ApiResponse<List<Section>>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Section> data = response.body().getData();
                    if (data != null && !data.isEmpty()) {
                        sections = data;
                        setupSectionSpinner();
                        return;
                    }
                }
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("No se encontraron secciones");
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Section>>> call, Throwable t) {
                showLoading(false);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Error de conexión");
            }
        });
    }

    private void setupSectionSpinner() {
        ArrayAdapter<Section> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sections);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSection.setAdapter(adapter);

        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSeccionId = sections.get(position).getId();
                fetchLeaderboard();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchLeaderboard() {
        if (currentSeccionId == -1) return;

        showLoading(true);
        tvEmpty.setVisibility(View.GONE);

        RetrofitClient.getApiService().getLeaderboard(currentSeccionId, currentTipo, currentMes)
                .enqueue(new Callback<ApiResponse<List<LeaderboardEntry>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<LeaderboardEntry>>> call, Response<ApiResponse<List<LeaderboardEntry>>> response) {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            List<LeaderboardEntry> data = response.body().getData();
                            if (data != null && !data.isEmpty()) {
                                entries.clear();
                                entries.addAll(data);
                                adapter.notifyDataSetChanged();
                                return;
                            }
                        }
                        entries.clear();
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("No hay datos para este período");
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<LeaderboardEntry>>> call, Throwable t) {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Error de conexión");
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) tvEmpty.setVisibility(View.GONE);
    }

    // ========== Adapter ==========

    private static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

        private final List<LeaderboardEntry> items;
        private String tipo;

        LeaderboardAdapter(List<LeaderboardEntry> items) {
            this.items = items;
            this.tipo = "puntual";
        }

        void setTipo(String tipo) {
            this.tipo = tipo;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_leaderboard, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LeaderboardEntry entry = items.get(position);
            int rank = position + 1;

            holder.tvPosition.setText(String.valueOf(rank));
            holder.tvName.setText(entry.getNombreCompleto());

            if (tipo == null) return;
            switch (tipo) {
                case "puntual":
                    holder.tvPercentage.setText(entry.getPuntualidad() + "%");
                    holder.tvStats.setText(entry.getPuntual() + "/" + entry.getTotalDias() + " puntual");
                    break;
                case "asistencia":
                    holder.tvPercentage.setText(entry.getAsistencia() + "%");
                    holder.tvStats.setText(entry.getAsistenciaDias() + "/" + entry.getTotalDias() + " asistió");
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPosition, tvName, tvPercentage, tvStats;

            ViewHolder(View itemView) {
                super(itemView);
                tvPosition = itemView.findViewById(R.id.tv_position);
                tvName = itemView.findViewById(R.id.tv_name);
                tvPercentage = itemView.findViewById(R.id.tv_percentage);
                tvStats = itemView.findViewById(R.id.tv_stats);
            }
        }
    }
}
