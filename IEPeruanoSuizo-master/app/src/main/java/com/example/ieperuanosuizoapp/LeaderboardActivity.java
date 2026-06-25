package com.example.ieperuanosuizoapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ieperuanosuizoapp.api.RetrofitClient;
import com.example.ieperuanosuizoapp.api.models.ApiResponse;
import com.example.ieperuanosuizoapp.api.models.LeaderboardEntry;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardActivity extends AppCompatActivity {

    private AutoCompleteTextView dropdownMonth, dropdownSection;
    private TabLayout tabLayoutLeaderboard;
    private SwitchMaterial switchClassroomFilter;
    private TextView tvActiveClassroomInfo, tvEmpty;
    private RecyclerView recyclerViewLeaderboard;
    private ProgressBar progressBar;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private View layoutClassroomFilter, layoutSectionSelector;

    private TextView tvName1, tvScore1, tvAvatar1, tvSuffix1;
    private TextView tvName2, tvScore2, tvAvatar2, tvSuffix2;
    private TextView tvName3, tvScore3, tvAvatar3, tvSuffix3;
    private View colRank1, colRank2, colRank3, podiumContainer;

    private String userId, userRol, userSeccionName;
    private String currentTipo = "asistencia";
    private String currentMes;
    private String mySeccionId = null;
    private boolean filterActive = false;
    private boolean isAdminOrProf = false;
    private Calendar currentCalendar;

    private List<Player> allPlayers = new ArrayList<>();
    private LeaderboardAdapter adapter;

    private List<String> sectionNames = new ArrayList<>();
    private List<String> sectionIds = new ArrayList<>();
    private String selectedSectionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        currentCalendar = Calendar.getInstance();
        currentMes = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.getTime());

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", "");
        userRol = prefs.getString("user_mode", "ALUMNO").toLowerCase();
        userSeccionName = prefs.getString("user_seccion", "");

        isAdminOrProf = userRol.contains("admin") || userRol.equals("profesor") || userRol.equals("docente");

        initViews();
        setupBackButton();
        setupTabLayout();
        setupSwipeRefresh();
        setupRecyclerView();
        setupMonthDropdown();
        loadSections();
    }

    private void initViews() {
        dropdownMonth = findViewById(R.id.dropdown_month);
        dropdownSection = findViewById(R.id.dropdown_section);
        tabLayoutLeaderboard = findViewById(R.id.tab_layout_leaderboard);
        switchClassroomFilter = findViewById(R.id.switch_classroom_filter);
        tvActiveClassroomInfo = findViewById(R.id.tv_active_classroom_info);
        tvEmpty = findViewById(R.id.tv_empty);
        recyclerViewLeaderboard = findViewById(R.id.recycler_view_leaderboard);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        podiumContainer = findViewById(R.id.podium_container);
        layoutClassroomFilter = findViewById(R.id.layout_classroom_filter);
        layoutSectionSelector = findViewById(R.id.layout_section_selector);

        colRank1 = findViewById(R.id.col_rank_1);
        colRank2 = findViewById(R.id.col_rank_2);
        colRank3 = findViewById(R.id.col_rank_3);

        tvName1 = findViewById(R.id.podium_name_1);
        tvScore1 = findViewById(R.id.podium_score_1);
        tvAvatar1 = findViewById(R.id.podium_avatar_text_1);
        tvSuffix1 = findViewById(R.id.podium_suffix_1);

        tvName2 = findViewById(R.id.podium_name_2);
        tvScore2 = findViewById(R.id.podium_score_2);
        tvAvatar2 = findViewById(R.id.podium_avatar_text_2);
        tvSuffix2 = findViewById(R.id.podium_suffix_2);

        tvName3 = findViewById(R.id.podium_name_3);
        tvScore3 = findViewById(R.id.podium_score_3);
        tvAvatar3 = findViewById(R.id.podium_avatar_text_3);
        tvSuffix3 = findViewById(R.id.podium_suffix_3);

        if (isAdminOrProf) {
            layoutClassroomFilter.setVisibility(View.GONE);
            layoutSectionSelector.setVisibility(View.VISIBLE);
        } else {
            layoutClassroomFilter.setVisibility(View.VISIBLE);
            layoutSectionSelector.setVisibility(View.GONE);
        }

        if (switchClassroomFilter != null) {
            switchClassroomFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
                filterActive = isChecked;
                if (tvActiveClassroomInfo != null) {
                    tvActiveClassroomInfo.setText(isChecked ? "Ver solo " + userSeccionName : "Ver todos los salones");
                }
                fetchLeaderboard(false);
            });
        }
    }

    private void setupBackButton() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupTabLayout() {
        tabLayoutLeaderboard.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTipo = tab.getPosition() == 0 ? "asistencia" : "puntual";
                adapter.setMetricSuffix(currentTipo.equals("asistencia") ? " d\u00edas" : " d\u00edas temprano");
                fetchLeaderboard();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> fetchLeaderboard());
        swipeRefresh.setColorSchemeResources(android.R.color.holo_red_dark);
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter();
        recyclerViewLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLeaderboard.setAdapter(adapter);
    }

    private void setupMonthDropdown() {
        List<String> months = new ArrayList<>();
        List<String> monthKeys = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE"));
        SimpleDateFormat fmtKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        for (int i = 0; i < 12; i++) {
            months.add(fmt.format(cal.getTime()));
            monthKeys.add(fmtKey.format(cal.getTime()));
            cal.add(Calendar.MONTH, 1);
        }

        currentCalendar = Calendar.getInstance();
        currentMes = fmtKey.format(currentCalendar.getTime());

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, months);
        dropdownMonth.setAdapter(monthAdapter);
        dropdownMonth.setText(fmt.format(currentCalendar.getTime()), false);

        dropdownMonth.setOnItemClickListener((parent, view, position, id) -> {
            currentMes = monthKeys.get(position);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, currentYear);
            c.set(Calendar.MONTH, position);
            currentCalendar = c;
            dropdownMonth.setText(months.get(position), false);
            fetchLeaderboard();
        });
    }

    private void loadSections() {
        RetrofitClient.getApiService().getSecciones(userId, userRol).enqueue(new Callback<ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Object>>> call, Response<ApiResponse<List<Object>>> response) {
                sectionNames.clear();
                sectionIds.clear();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Object> data = response.body().getData();
                    if (data != null) {
                        Gson gson = new Gson();
                        for (Object obj : data) {
                            try {
                                JsonObject json = gson.toJsonTree(obj).getAsJsonObject();
                                String id = json.has("id") ? json.get("id").getAsString() : "";
                                String nombre = json.has("nombre") ? json.get("nombre").getAsString() : "";
                                String gradoNombre = "";
                                if (json.has("grados") && json.get("grados").isJsonObject()) {
                                    gradoNombre = json.getAsJsonObject("grados").get("nombre").getAsString();
                                }
                                String fullName = gradoNombre.isEmpty() ? nombre : gradoNombre + " " + nombre;
                                if (!id.isEmpty()) {
                                    if (!isAdminOrProf) {
                                        if (fullName.contains(userSeccionName)) {
                                            mySeccionId = id;
                                        }
                                    } else {
                                        sectionNames.add(fullName);
                                        sectionIds.add(id);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
                if (isAdminOrProf) {
                    setupSectionDropdown();
                } else {
                    if (tvActiveClassroomInfo != null)
                        tvActiveClassroomInfo.setText("Ver todos los salones");
                    fetchLeaderboard();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Object>>> call, Throwable t) {
                if (isAdminOrProf) {
                    setupSectionDropdown();
                } else {
                    if (tvActiveClassroomInfo != null)
                        tvActiveClassroomInfo.setText("Ver todos los salones");
                    fetchLeaderboard();
                }
            }
        });
    }

    private void setupSectionDropdown() {
        List<String> displayNames = new ArrayList<>();
        displayNames.add("Todos los salones");
        displayNames.addAll(sectionNames);

        List<String> ids = new ArrayList<>();
        ids.add(null);
        ids.addAll(sectionIds);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, displayNames);
        dropdownSection.setAdapter(adapter);
        dropdownSection.setText("Todos los salones", false);

        dropdownSection.setOnItemClickListener((parent, view, position, id) -> {
            selectedSectionId = ids.get(position);
            dropdownSection.setText(displayNames.get(position), false);
            fetchLeaderboard();
        });

        selectedSectionId = null;
        fetchLeaderboard();
    }

    private void fetchLeaderboard() {
        fetchLeaderboard(true);
    }

    private void fetchLeaderboard(boolean showLoader) {
        if (showLoader) showLoading(true);
        tvEmpty.setVisibility(View.GONE);
        podiumContainer.setVisibility(View.GONE);
        adapter.submitList(new ArrayList<>());

        String seccionParam;
        if (isAdminOrProf) {
            seccionParam = selectedSectionId;
        } else {
            seccionParam = filterActive ? mySeccionId : null;
        }

        Call<ApiResponse<List<LeaderboardEntry>>> call;
        if (seccionParam == null) {
            call = RetrofitClient.getApiService().getLeaderboardAll(currentTipo, currentMes);
        } else {
            call = RetrofitClient.getApiService().getLeaderboard(seccionParam, currentTipo, currentMes);
        }

        call.enqueue(new Callback<ApiResponse<List<LeaderboardEntry>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LeaderboardEntry>>> call, Response<ApiResponse<List<LeaderboardEntry>>> response) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                tvEmpty.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    allPlayers = entriesToPlayers(response.body().getData());
                    renderLeaderboard();
                } else {
                    allPlayers.clear();
                    adapter.submitList(new ArrayList<>());
                    podiumContainer.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("No hay datos para este per\u00edodo");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LeaderboardEntry>>> call, Throwable t) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                allPlayers.clear();
                adapter.submitList(new ArrayList<>());
                podiumContainer.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Sin conexi\u00f3n");
            }
        });
    }

    private List<Player> entriesToPlayers(List<LeaderboardEntry> entries) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry e = entries.get(i);
            int score = currentTipo.equals("asistencia") ? e.getAsistenciaDias() : e.getPuntual();
            players.add(new Player(
                e.getPersonaId(),
                e.getPrimerNombre(),
                score,
                0,
                e.getSalon() != null ? e.getSalon() : "",
                String.valueOf(e.getSeccionId())
            ));
        }
        for (int i = 0; i < players.size(); i++) players.get(i).setRank(i + 1);
        return players;
    }

    private void renderLeaderboard() {
        if (allPlayers.isEmpty()) return;

        String suffix = currentTipo.equals("asistencia") ? " d\u00edas" : " d\u00edas temprano";
        adapter.setMetricSuffix(suffix);
        podiumContainer.setVisibility(View.VISIBLE);

        if (allPlayers.size() >= 1) {
            Player p = allPlayers.get(0);
            tvName1.setText(p.getName());
            tvAvatar1.setText(p.getAvatarInitials());
            tvScore1.setText(String.valueOf(p.getScore()));
            tvSuffix1.setText(suffix);
        }
        if (allPlayers.size() >= 2) {
            Player p = allPlayers.get(1);
            tvName2.setText(p.getName());
            tvAvatar2.setText(p.getAvatarInitials());
            tvScore2.setText(String.valueOf(p.getScore()));
            tvSuffix2.setText(suffix);
            colRank2.setVisibility(View.VISIBLE);
        } else {
            colRank2.setVisibility(View.INVISIBLE);
        }
        if (allPlayers.size() >= 3) {
            Player p = allPlayers.get(2);
            tvName3.setText(p.getName());
            tvAvatar3.setText(p.getAvatarInitials());
            tvScore3.setText(String.valueOf(p.getScore()));
            tvSuffix3.setText(suffix);
            colRank3.setVisibility(View.VISIBLE);
        } else {
            colRank3.setVisibility(View.INVISIBLE);
        }

        List<Player> remaining = new ArrayList<>();
        if (allPlayers.size() > 3) {
            for (int i = 3; i < allPlayers.size(); i++) {
                Player p = allPlayers.get(i);
                p.setRank(i + 1);
                remaining.add(p);
            }
        }
        adapter.submitList(remaining);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) tvEmpty.setVisibility(View.GONE);
    }
}
