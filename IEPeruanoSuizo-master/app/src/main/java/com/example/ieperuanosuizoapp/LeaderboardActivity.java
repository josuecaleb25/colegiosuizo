package com.example.ieperuanosuizoapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView autoCompleteSection;
    private RecyclerView rvLeaderboard;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private View sectionHeader, statusLayout, podiumContainer;
    private TextView tvName1, tvScore1, tvName2, tvScore2, tvName3, tvScore3;
    private TextView tvMonthLabel;
    private ImageButton btnPrevMonth, btnNextMonth;

    private String userId;
    private String userRol;
    private String currentTipo = "puntual";
    private String currentMes;
    private int currentSeccionId = -1;
    private Calendar currentCalendar;

    private List<Section> sections = new ArrayList<>();
    private List<LeaderboardEntry> entries = new ArrayList<>();
    private LeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        currentCalendar = Calendar.getInstance();
        currentMes = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.getTime());

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
        autoCompleteSection = findViewById(R.id.auto_complete_section);
        rvLeaderboard = findViewById(R.id.rv_leaderboard);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        sectionHeader = findViewById(R.id.section_header);
        statusLayout = findViewById(R.id.status_layout);
        podiumContainer = findViewById(R.id.podium_container);
        tvMonthLabel = findViewById(R.id.tv_month_label);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);

        tvName1 = findViewById(R.id.tv_name_1);
        tvScore1 = findViewById(R.id.tv_score_1);
        tvName2 = findViewById(R.id.tv_name_2);
        tvScore2 = findViewById(R.id.tv_score_2);
        tvName3 = findViewById(R.id.tv_name_3);
        tvScore3 = findViewById(R.id.tv_score_3);

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonth();
        });
        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateMonth();
        });

        // Inicializar label del mes
        SimpleDateFormat displayFmt = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE"));
        String label = displayFmt.format(currentCalendar.getTime());
        label = label.substring(0, 1).toUpperCase() + label.substring(1);
        tvMonthLabel.setText(label);
    }

    private void updateMonth() {
        currentMes = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.getTime());
        SimpleDateFormat displayFmt = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE"));
        String label = displayFmt.format(currentCalendar.getTime());
        label = label.substring(0, 1).toUpperCase() + label.substring(1);
        tvMonthLabel.setText(label);
        fetchLeaderboard();
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
                fetchLeaderboard();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> fetchLeaderboard());
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
                    if (data != null) sections = data;
                }
                setupSectionSelector();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Section>>> call, Throwable t) {
                showLoading(false);
                setupSectionSelector();
            }
        });
    }

    private void setupSectionSelector() {
        List<String> names = new ArrayList<>();
        names.add("Todos");
        for (Section s : sections) {
            names.add(s.getNombre());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        autoCompleteSection.setAdapter(adapter);

        autoCompleteSection.setText("Todos", false);
        currentSeccionId = -1;
        fetchLeaderboard();

        autoCompleteSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    currentSeccionId = -1;
                } else {
                    currentSeccionId = sections.get(position - 1).getId();
                }
                fetchLeaderboard();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchLeaderboard() {
        showLoading(true);
        tvEmpty.setVisibility(View.GONE);
        sectionHeader.setVisibility(View.GONE);
        statusLayout.setVisibility(View.GONE);
        podiumContainer.setVisibility(View.GONE);

        retrofit2.Call<ApiResponse<List<LeaderboardEntry>>> call;
        if (currentSeccionId == -1) {
            call = RetrofitClient.getApiService().getLeaderboardAll(currentTipo, currentMes);
        } else {
            call = RetrofitClient.getApiService().getLeaderboard(currentSeccionId, currentTipo, currentMes);
        }

        call.enqueue(new Callback<ApiResponse<List<LeaderboardEntry>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<LeaderboardEntry>>> call, Response<ApiResponse<List<LeaderboardEntry>>> response) {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                         if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null && !response.body().getData().isEmpty()) {
                            entries.clear();
                            entries.addAll(response.body().getData());
                            adapter.notifyDataSetChanged();
                            sectionHeader.setVisibility(View.VISIBLE);
                            statusLayout.setVisibility(View.VISIBLE);
                            updatePodium();
                        } else {
                            entries.clear();
                            adapter.notifyDataSetChanged();
                            sectionHeader.setVisibility(View.VISIBLE);
                            statusLayout.setVisibility(View.GONE);
                            podiumContainer.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("No hay datos para este período");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<LeaderboardEntry>>> call, Throwable t) {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        entries.clear();
                        adapter.notifyDataSetChanged();
                        sectionHeader.setVisibility(View.GONE);
                        statusLayout.setVisibility(View.GONE);
                        podiumContainer.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Sin conexión");
                    }
                });
    }

    private void updatePodium() {
        if (entries.isEmpty()) return;
        podiumContainer.setVisibility(View.VISIBLE);

        // Top 1
        LeaderboardEntry first = entries.get(0);
        tvName1.setText(first.getPrimerNombre());
        tvScore1.setText(getScoreText(first));

        // Top 2
        if (entries.size() > 1) {
            LeaderboardEntry second = entries.get(1);
            tvName2.setText(second.getPrimerNombre());
            tvScore2.setText(getScoreText(second));
            findViewById(R.id.podium_2).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.podium_2).setVisibility(View.INVISIBLE);
        }

        // Top 3
        if (entries.size() > 2) {
            LeaderboardEntry third = entries.get(2);
            tvName3.setText(third.getPrimerNombre());
            tvScore3.setText(getScoreText(third));
            findViewById(R.id.podium_3).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.podium_3).setVisibility(View.INVISIBLE);
        }
    }

    private String getScoreText(LeaderboardEntry e) {
        String emoji = "asistencia".equals(currentTipo) ? "\u2B50" : "\uD83D\uDD25";
        int count = "asistencia".equals(currentTipo) ? e.getAsistenciaDias() : e.getPuntual();
        return emoji + " " + count;
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
            int adjustedPosition = position + 3;
            if (adjustedPosition >= items.size()) return;
            LeaderboardEntry entry = items.get(adjustedPosition);
            int rank = adjustedPosition + 1;

            holder.tvPosition.setText(String.valueOf(rank));
            holder.tvName.setText(entry.getPrimerNombre());

            if (tipo == null) return;
            String emoji = "asistencia".equals(tipo) ? "\u2B50" : "\uD83D\uDD25";
            switch (tipo) {
                case "puntual":
                    holder.tvPercentage.setText(emoji + " " + entry.getPuntual());
                    holder.tvStats.setText(entry.getSalon());
                    break;
                case "asistencia":
                    holder.tvPercentage.setText(emoji + " " + entry.getAsistenciaDias());
                    holder.tvStats.setText(entry.getSalon());
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return Math.max(0, items.size() - 3);
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
