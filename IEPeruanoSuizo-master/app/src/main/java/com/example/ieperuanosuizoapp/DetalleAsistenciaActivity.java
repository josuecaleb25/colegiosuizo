package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFPieChartData;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDPt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPieChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPieSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DetalleAsistenciaActivity extends AppCompatActivity {

    public static List<AsistenciaAlumno> dataTransfer = null;
    public static String dateTransfer = "";

    private TextView tvTitulo, tvTotal, tvATiempo, tvTardanza, tvAusentes, tvPagination;
    private EditText etSearch;
    private RecyclerView rvDetalle;
    private LinearLayout layoutFabMenu;
    private MaterialAutoCompleteTextView autoCompleteSalonFilter;
    private boolean isFabMenuOpen = false;

    private List<AsistenciaAlumno> allAlumnos = new ArrayList<>();
    private List<AsistenciaAlumno> filteredAlumnos = new ArrayList<>();
    private DetalleAdapter adapter;

    private int currentPage = 1;
    private final int PAGE_SIZE = 10;
    private String selectedSalon = null;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_asistencia);

        if (dataTransfer != null) {
            allAlumnos = new ArrayList<>(dataTransfer);
            filteredAlumnos = new ArrayList<>(allAlumnos);
            dataTransfer = null;
        }

        initViews();
        setupRecyclerView();
        setupSearch();
        setupSalonFilter();
        setupPagination();
        setupFab();
        updateSummary();
        updateList();
    }

    private void initViews() {
        tvTitulo = findViewById(R.id.tv_titulo_fecha);
        tvTotal = findViewById(R.id.tv_total_alumnos);
        tvATiempo = findViewById(R.id.tv_a_tiempo);
        tvTardanza = findViewById(R.id.tv_tardanza);
        tvAusentes = findViewById(R.id.tv_ausentes);
        tvPagination = findViewById(R.id.tv_pagination_info);
        etSearch = findViewById(R.id.et_search);
        rvDetalle = findViewById(R.id.rv_asistencia_detalle);
        layoutFabMenu = findViewById(R.id.layout_fab_menu);
        autoCompleteSalonFilter = findViewById(R.id.auto_complete_salon_filter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        String formattedDate = formatTituloLargo(dateTransfer);
        tvTitulo.setText("Asistencia - " + formattedDate);
    }

    private void setupRecyclerView() {
        adapter = new DetalleAdapter(new ArrayList<>());
        rvDetalle.setLayoutManager(new LinearLayoutManager(this));
        rvDetalle.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSalonFilter() {
        Set<String> uniqueSalones = new LinkedHashSet<>();
        for (AsistenciaAlumno a : allAlumnos) {
            if (a.getSalon() != null && !a.getSalon().isEmpty()) {
                uniqueSalones.add(a.getSalon());
            }
        }

        List<String> salones = new ArrayList<>();
        salones.add("Todos");
        salones.addAll(uniqueSalones);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, salones);
        autoCompleteSalonFilter.setAdapter(adapter);
        autoCompleteSalonFilter.setText("Todos", false);

        autoCompleteSalonFilter.setOnItemClickListener((parent, view, position, id) -> {
            String selected = salones.get(position);
            selectedSalon = selected.equals("Todos") ? null : selected;
            autoCompleteSalonFilter.setText(selected, false);
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredAlumnos.clear();
        for (AsistenciaAlumno a : allAlumnos) {
            boolean matchesSalon = selectedSalon == null || a.getSalon() != null && a.getSalon().equals(selectedSalon);
            boolean matchesSearch = searchQuery.isEmpty() || a.getNombre_completo().toLowerCase().contains(searchQuery.toLowerCase());
            if (matchesSalon && matchesSearch) {
                filteredAlumnos.add(a);
            }
        }
        currentPage = 1;
        updateList();
    }

    private void setupPagination() {
        findViewById(R.id.btn_prev_page).setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updateList();
            }
        });

        findViewById(R.id.btn_next_page).setOnClickListener(v -> {
            int totalPages = (int) Math.ceil((double) filteredAlumnos.size() / PAGE_SIZE);
            if (currentPage < totalPages) {
                currentPage++;
                updateList();
            }
        });
    }

    private void updateList() {
        int totalPages = (int) Math.ceil((double) filteredAlumnos.size() / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        tvPagination.setText("Pagina " + currentPage + " de " + totalPages);

        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredAlumnos.size());
        
        List<AsistenciaAlumno> pageItems = new ArrayList<>();
        if (start < filteredAlumnos.size()) {
            pageItems.addAll(filteredAlumnos.subList(start, end));
        }
        adapter.updateData(pageItems);
    }

    private void updateSummary() {
        int total = allAlumnos.size();
        int at = 0, tard = 0, aus = 0;
        for (AsistenciaAlumno a : allAlumnos) {
            boolean tieneHora = a.getHora_registro() != null && !a.getHora_registro().isEmpty();
            if (!tieneHora) {
                aus++;
            } else {
                String e = a.getEstado_entrada() != null ? a.getEstado_entrada().toLowerCase(Locale.ROOT) : "";
                if (e.contains("tardanza")) tard++;
                else at++;
            }
        }
        tvTotal.setText(String.valueOf(total));
        tvATiempo.setText(String.valueOf(at));
        tvTardanza.setText(String.valueOf(tard));
        tvAusentes.setText(String.valueOf(aus));
    }

    private void setupFab() {
        findViewById(R.id.fab_main).setOnClickListener(v -> toggleFabMenu());
        findViewById(R.id.btn_export_csv).setOnClickListener(v -> {
            descargarExcel();
            toggleFabMenu();
        });

    }

    private void toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen;
        if (isFabMenuOpen) {
            showFabMenu();
        } else {
            hideFabMenu();
        }
    }

    private void showFabMenu() {
        layoutFabMenu.setVisibility(View.VISIBLE);
        FloatingActionButton mainFab = findViewById(R.id.fab_main);
        mainFab.setImageResource(R.drawable.ic_close);

        layoutFabMenu.setAlpha(0f);
        layoutFabMenu.setTranslationY(40f);
        layoutFabMenu.setScaleX(0.7f);
        layoutFabMenu.setScaleY(0.7f);
        layoutFabMenu.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .setInterpolator(new FastOutSlowInInterpolator())
            .start();
    }

    private void hideFabMenu() {
        FloatingActionButton mainFab = findViewById(R.id.fab_main);
        mainFab.setImageResource(R.drawable.ic_download);

        layoutFabMenu.animate()
            .alpha(0f)
            .translationY(20f)
            .scaleX(0.7f)
            .scaleY(0.7f)
            .setDuration(150)
            .setInterpolator(new FastOutLinearInInterpolator())
            .withEndAction(() -> {
                if (!isFabMenuOpen) {
                    layoutFabMenu.setVisibility(View.GONE);
                }
            })
            .start();
    }

    private void descargarExcel() {
        if (allAlumnos.isEmpty()) return;
        try {
            Map<String, List<AsistenciaAlumno>> bySalon = new LinkedHashMap<>();
            for (AsistenciaAlumno a : allAlumnos) {
                String salon = a.getSalon() != null && !a.getSalon().isEmpty() ? a.getSalon() : "Sin salón";
                bySalon.computeIfAbsent(salon, k -> new ArrayList<>()).add(a);
            }

            String fileName = "asistencia_" + dateTransfer.replace("-", "_") + ".xlsx";
            File file = new File(getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);

            XSSFWorkbook wb = new XSSFWorkbook();
            createResumenSheet(wb, bySalon.size());
            for (Map.Entry<String, List<AsistenciaAlumno>> e : bySalon.entrySet()) {
                createSalonSheet(wb, e.getKey(), e.getValue());
            }
            wb.write(fos);
            wb.close();
            fos.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Compartir Excel"));
        } catch (Throwable e) {
            android.util.Log.e("ExcelExport", "Error generando Excel", e);
            Toast.makeText(this, "Error al generar Excel: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createResumenSheet(XSSFWorkbook wb, int salonCount) throws Exception {
        XSSFSheet sheet = wb.createSheet("Resumen");

        int total = allAlumnos.size();
        int at = 0, tard = 0, aus = 0;
        for (AsistenciaAlumno a : allAlumnos) {
            boolean tieneHora = a.getHora_registro() != null && !a.getHora_registro().isEmpty();
            if (!tieneHora) aus++;
            else if (a.getEstado_entrada() != null && a.getEstado_entrada().toLowerCase().contains("tardanza")) tard++;
            else at++;
        }
        double pctAt = total == 0 ? 0 : at * 100.0 / total;
        double pctTard = total == 0 ? 0 : tard * 100.0 / total;
        double pctAus = total == 0 ? 0 : aus * 100.0 / total;

        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(titleFont);

        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 12);
        CellStyle boldStyle = wb.createCellStyle();
        boldStyle.setFont(boldFont);

        CellStyle hdrStyle = wb.createCellStyle();
        hdrStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        hdrStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font hdrFont = wb.createFont();
        hdrFont.setColor(IndexedColors.WHITE.getIndex());
        hdrFont.setBold(true);
        hdrStyle.setFont(hdrFont);

        CellStyle centerStyle = wb.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);

        sheet.createRow(0).createCell(0).setCellValue("REPORTE DE ASISTENCIA");
        sheet.getRow(0).getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));
        sheet.createRow(1).createCell(0).setCellValue("Fecha: " + formatTituloLargo(dateTransfer));
        sheet.getRow(1).getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 2));

        sheet.createRow(3).createCell(0).setCellValue("ESTADÍSTICAS GENERALES");
        sheet.getRow(3).getCell(0).setCellStyle(boldStyle);
        int r = 4;
        sheet.createRow(r).createCell(0).setCellValue("Total alumnos");
        sheet.getRow(r).createCell(1).setCellValue(total);
        sheet.createRow(++r).createCell(0).setCellValue("A tiempo");
        sheet.getRow(r).createCell(1).setCellValue(at);
        sheet.getRow(r).createCell(2).setCellValue(String.format("%.1f%%", pctAt));
        sheet.createRow(++r).createCell(0).setCellValue("Tardanza");
        sheet.getRow(r).createCell(1).setCellValue(tard);
        sheet.getRow(r).createCell(2).setCellValue(String.format("%.1f%%", pctTard));
        sheet.createRow(++r).createCell(0).setCellValue("Ausentes");
        sheet.getRow(r).createCell(1).setCellValue(aus);
        sheet.getRow(r).createCell(2).setCellValue(String.format("%.1f%%", pctAus));
        sheet.createRow(++r).createCell(0).setCellValue("Salones");
        sheet.getRow(r).createCell(1).setCellValue(salonCount);

        // Chart data rows
        int cr = 11;
        CellStyle hdrCenterStyle = wb.createCellStyle();
        hdrCenterStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        hdrCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        hdrCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        Font hdrCenterFont = wb.createFont();
        hdrCenterFont.setColor(IndexedColors.WHITE.getIndex());
        hdrCenterFont.setBold(true);
        hdrCenterStyle.setFont(hdrCenterFont);

        sheet.createRow(cr - 1).createCell(0).setCellValue("ESTADO");
        sheet.getRow(cr - 1).getCell(0).setCellStyle(hdrStyle);
        sheet.getRow(cr - 1).createCell(1).setCellValue("CANTIDAD");
        sheet.getRow(cr - 1).getCell(1).setCellStyle(hdrCenterStyle);
        sheet.createRow(cr).createCell(0).setCellValue("A tiempo");
        sheet.getRow(cr).createCell(1).setCellValue(at);
        sheet.getRow(cr).getCell(1).setCellStyle(centerStyle);
        sheet.createRow(cr + 1).createCell(0).setCellValue("Tardanza");
        sheet.getRow(cr + 1).createCell(1).setCellValue(tard);
        sheet.getRow(cr + 1).getCell(1).setCellStyle(centerStyle);
        sheet.createRow(cr + 2).createCell(0).setCellValue("Ausentes");
        sheet.getRow(cr + 2).createCell(1).setCellValue(aus);
        sheet.getRow(cr + 2).getCell(1).setCellStyle(centerStyle);

        // Pie chart
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor pieAnchor = new XSSFClientAnchor(0, 0, 0, 0, 4, 3, 14, 17);
        XSSFChart pieChart = drawing.createChart(pieAnchor);
        pieChart.setTitleText("Resumen de Asistencia");
        XDDFChartLegend pieLegend = pieChart.getOrAddLegend();
        pieLegend.setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis pieCatAxis = pieChart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis pieValAxis = pieChart.createValueAxis(AxisPosition.LEFT);
        XDDFPieChartData pie = (XDDFPieChartData) pieChart.createData(ChartTypes.PIE, pieCatAxis, pieValAxis);

        XDDFDataSource<String> cats = XDDFDataSourcesFactory.fromStringCellRange(sheet,
            new org.apache.poi.ss.util.CellRangeAddress(cr, cr + 2, 0, 0));
        XDDFNumericalDataSource<Double> vals = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
            new org.apache.poi.ss.util.CellRangeAddress(cr, cr + 2, 1, 1));
        pie.addSeries(cats, vals);
        pieChart.plot(pie);
        try {
            setPieSliceColors(pieChart, new byte[][]{
                {(byte)0x27, (byte)0xAE, (byte)0x60},
                {(byte)0xFF, (byte)0x98, (byte)0x00},
                {(byte)0xBA, (byte)0x19, (byte)0x24}
            });
        } catch (Throwable t) {
            android.util.Log.e("ExcelChart", "setPieSliceColors Resumen failed", t);
        }

        // Bar chart
        XSSFClientAnchor barAnchor = new XSSFClientAnchor(0, 0, 0, 0, 4, 18, 14, 31);
        XSSFChart barChart = drawing.createChart(barAnchor);
        barChart.setTitleText("Comparativa por Estado");
        XDDFChartLegend barLegend = barChart.getOrAddLegend();
        barLegend.setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis barCatAxis = barChart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis barValAxis = barChart.createValueAxis(AxisPosition.LEFT);
        XDDFBarChartData bar = (XDDFBarChartData) barChart.createData(ChartTypes.BAR, barCatAxis, barValAxis);
        bar.setBarDirection(BarDirection.COL);
        XDDFDataSource<String> barCats = XDDFDataSourcesFactory.fromStringCellRange(sheet,
            new org.apache.poi.ss.util.CellRangeAddress(cr, cr + 2, 0, 0));
        XDDFNumericalDataSource<Double> barVals = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
            new org.apache.poi.ss.util.CellRangeAddress(cr, cr + 2, 1, 1));
        XDDFBarChartData.Series barSeries = (XDDFBarChartData.Series) bar.addSeries(barCats, barVals);
        barSeries.setTitle("Alumnos", null);
        barChart.plot(bar);
        try {
            setBarSeriesColor(barChart, new byte[]{(byte)0xBA, (byte)0x19, (byte)0x24});
        } catch (Throwable t) {
            android.util.Log.e("ExcelChart", "setBarSeriesColor failed", t);
        }

        autoSizeColumns(sheet, 0, 3);
    }

    private void createSalonSheet(XSSFWorkbook wb, String salonName, List<AsistenciaAlumno> alumnos) throws Exception {
        XSSFSheet sheet = wb.createSheet(salonName.length() <= 31 ? salonName : salonName.substring(0, 31));

        CellStyle hdrStyle = wb.createCellStyle();
        hdrStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        hdrStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font hdrFont = wb.createFont();
        hdrFont.setColor(IndexedColors.WHITE.getIndex());
        hdrFont.setBold(true);
        hdrStyle.setFont(hdrFont);

        CellStyle hdrCenterStyle = wb.createCellStyle();
        hdrCenterStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        hdrCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        hdrCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        Font hdrCenterFont = wb.createFont();
        hdrCenterFont.setColor(IndexedColors.WHITE.getIndex());
        hdrCenterFont.setBold(true);
        hdrCenterStyle.setFont(hdrCenterFont);

        CellStyle centerStyle = wb.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle redStyle = wb.createCellStyle();
        redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        redStyle.setAlignment(HorizontalAlignment.CENTER);
        Font whiteFont = wb.createFont();
        whiteFont.setColor(IndexedColors.WHITE.getIndex());
        redStyle.setFont(whiteFont);

        CellStyle orangeStyle = wb.createCellStyle();
        orangeStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        orangeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        orangeStyle.setAlignment(HorizontalAlignment.CENTER);
        Font blackFont = wb.createFont();
        blackFont.setColor(IndexedColors.BLACK.getIndex());
        orangeStyle.setFont(blackFont);

        CellStyle greenStyle = wb.createCellStyle();
        greenStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
        greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        greenStyle.setAlignment(HorizontalAlignment.CENTER);
        greenStyle.setFont(whiteFont);

        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(titleFont);

        int at = 0, tard = 0, aus = 0;
        for (AsistenciaAlumno a : alumnos) {
            boolean tieneHora = a.getHora_registro() != null && !a.getHora_registro().isEmpty();
            if (!tieneHora) aus++;
            else if (a.getEstado_entrada() != null && a.getEstado_entrada().toLowerCase().contains("tardanza")) tard++;
            else at++;
        }
        int total = at + tard + aus;

        sheet.createRow(0).createCell(0).setCellValue("Salón: " + salonName);
        sheet.getRow(0).getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));
        int r = 2;
        sheet.createRow(r).createCell(0).setCellValue("Total");
        sheet.getRow(r).createCell(1).setCellValue(total);
        sheet.createRow(++r).createCell(0).setCellValue("A tiempo");
        sheet.getRow(r).createCell(1).setCellValue(at);
        sheet.createRow(++r).createCell(0).setCellValue("Tardanza");
        sheet.getRow(r).createCell(1).setCellValue(tard);
        sheet.createRow(++r).createCell(0).setCellValue("Ausentes");
        sheet.getRow(r).createCell(1).setCellValue(aus);

        // Table header
        r = 7;
        sheet.createRow(r).createCell(0).setCellValue("Estudiante");
        sheet.getRow(r).getCell(0).setCellStyle(hdrStyle);
        sheet.getRow(r).createCell(1).setCellValue("Entrada");
        sheet.getRow(r).getCell(1).setCellStyle(hdrCenterStyle);
        sheet.getRow(r).createCell(2).setCellValue("Estado");
        sheet.getRow(r).getCell(2).setCellStyle(hdrCenterStyle);

        r = 8;
        for (AsistenciaAlumno a : alumnos) {
            sheet.createRow(r).createCell(0).setCellValue(a.getNombre_completo());
            String hora = a.getHora_registro();
            CellStyle style;
            String display, label;
            if (hora == null || hora.isEmpty()) {
                display = "--";
                style = redStyle;
                label = "Ausente";
            } else if (a.getEstado_entrada() != null && a.getEstado_entrada().toLowerCase().contains("tardanza")) {
                display = hora;
                style = orangeStyle;
                label = "Tardanza";
            } else {
                display = hora;
                style = greenStyle;
                label = "A tiempo";
            }
            sheet.getRow(r).createCell(1).setCellValue(display);
            sheet.getRow(r).getCell(1).setCellStyle(style);
            sheet.getRow(r).createCell(2).setCellValue(label);
            sheet.getRow(r).getCell(2).setCellStyle(style);
            r++;
        }

        // Chart data rows
        int cr = r + 2;
        Font chartHdrFont = wb.createFont();
        chartHdrFont.setBold(true);
        chartHdrFont.setFontHeightInPoints((short) 10);
        CellStyle chartHdrStyle = wb.createCellStyle();
        chartHdrStyle.setFont(chartHdrFont);

        CellStyle chartHdrCenter = wb.createCellStyle();
        chartHdrCenter.setFont(chartHdrFont);
        chartHdrCenter.setAlignment(HorizontalAlignment.CENTER);

        sheet.createRow(cr).createCell(0).setCellValue("ESTADO");
        sheet.getRow(cr).getCell(0).setCellStyle(chartHdrStyle);
        sheet.getRow(cr).createCell(1).setCellValue("CANTIDAD");
        sheet.getRow(cr).getCell(1).setCellStyle(chartHdrCenter);
        sheet.createRow(cr + 1).createCell(0).setCellValue("A tiempo");
        sheet.getRow(cr + 1).createCell(1).setCellValue(at);
        sheet.getRow(cr + 1).getCell(1).setCellStyle(centerStyle);
        sheet.createRow(cr + 2).createCell(0).setCellValue("Tardanza");
        sheet.getRow(cr + 2).createCell(1).setCellValue(tard);
        sheet.getRow(cr + 2).getCell(1).setCellStyle(centerStyle);
        sheet.createRow(cr + 3).createCell(0).setCellValue("Ausentes");
        sheet.getRow(cr + 3).createCell(1).setCellValue(aus);
        sheet.getRow(cr + 3).getCell(1).setCellStyle(centerStyle);

        // Pie chart for this salon
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, 4, 0, 12, 10);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Asistencia - " + salonName);
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFDataSource<String> cats = XDDFDataSourcesFactory.fromStringCellRange(sheet,
            new org.apache.poi.ss.util.CellRangeAddress(cr + 1, cr + 3, 0, 0));
        XDDFNumericalDataSource<Double> vals = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
            new org.apache.poi.ss.util.CellRangeAddress(cr + 1, cr + 3, 1, 1));
        XDDFCategoryAxis catAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis valAxis = chart.createValueAxis(AxisPosition.LEFT);
        XDDFPieChartData pie = (XDDFPieChartData) chart.createData(ChartTypes.PIE, catAxis, valAxis);
        pie.addSeries(cats, vals);
        chart.plot(pie);
        try {
            setPieSliceColors(chart, new byte[][]{
                {(byte)0x27, (byte)0xAE, (byte)0x60},
                {(byte)0xFF, (byte)0x98, (byte)0x00},
                {(byte)0xBA, (byte)0x19, (byte)0x24}
            });
        } catch (Throwable t) {
            android.util.Log.e("ExcelChart", "setPieSliceColors Salon failed", t);
        }

        autoSizeColumns(sheet, 0, 3);
    }

    private void autoSizeColumns(XSSFSheet sheet, int startCol, int endCol) {
        org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
        Paint paint = new Paint();
        paint.setTextSize(14.667f);
        for (int col = startCol; col < endCol; col++) {
            float maxWidth = 0;
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row != null) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(col);
                    if (cell != null) {
                        String val = formatter.formatCellValue(cell);
                        if (val != null) {
                            float w = paint.measureText(val);
                            if (w > maxWidth) maxWidth = w;
                        }
                    }
                }
            }
            if (maxWidth > 0) {
                sheet.setColumnWidth(col, Math.round(maxWidth / 7f * 256 + 512));
            }
        }
    }

    private void setPieSliceColors(XSSFChart chart, byte[][] colors) {
        CTChart ctChart = chart.getCTChart();
        CTPlotArea plotArea = ctChart.getPlotArea();
        CTPieChart pieChart = plotArea.getPieChartArray(0);
        CTPieSer ser = pieChart.getSerArray(0);
        for (int i = 0; i < Math.min(colors.length, 3); i++) {
            CTDPt dPt = ser.addNewDPt();
            CTUnsignedInt idx = CTUnsignedInt.Factory.newInstance();
            idx.setVal(i);
            dPt.setIdx(idx);
            CTShapeProperties spPr = dPt.addNewSpPr();
            CTSolidColorFillProperties fill = spPr.addNewSolidFill();
            CTSRgbColor srgbClr = fill.addNewSrgbClr();
            srgbClr.setVal(colors[i]);
        }
    }

    private void setBarSeriesColor(XSSFChart chart, byte[] rgb) {
        CTChart ctChart = chart.getCTChart();
        CTPlotArea plotArea = ctChart.getPlotArea();
        CTBarChart barChart = plotArea.getBarChartArray(0);
        CTBarSer ser = barChart.getSerArray(0);
        CTShapeProperties spPr = ser.addNewSpPr();
        CTSolidColorFillProperties fill = spPr.addNewSolidFill();
        CTSRgbColor srgbClr = fill.addNewSrgbClr();
        srgbClr.setVal(rgb);
    }

    private String formatTituloLargo(String fechaIso) {
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = iso.parse(fechaIso);
            SimpleDateFormat fmt = new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "PE"));
            return fmt.format(d);
        } catch (ParseException e) {
            return fechaIso;
        }
    }

    // ========== Adapter ==========
    private static class DetalleAdapter extends RecyclerView.Adapter<DetalleAdapter.ViewHolder> {
        private final List<AsistenciaAlumno> items;
        DetalleAdapter(List<AsistenciaAlumno> items) { this.items = items; }
        void updateData(List<AsistenciaAlumno> newData) {
            items.clear();
            items.addAll(newData);
            notifyDataSetChanged();
        }
        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detalle_asistencia_fila, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AsistenciaAlumno a = items.get(position);
            holder.tvNombre.setText(a.getNombre_completo());
            holder.tvSalon.setText(a.getSalon());
            String hora = a.getHora_registro();
            if (hora == null || hora.isEmpty()) {
                holder.tvEntrada.setText("Ausente");
                holder.tvEntrada.setTextColor(0xFFBA1924);
            } else {
                holder.tvEntrada.setText(hora);
                String est = a.getEstado_entrada() != null ? a.getEstado_entrada().toLowerCase() : "";
                holder.tvEntrada.setTextColor(est.contains("tardanza") ? 0xFFFF9800 : 0xFF27AE60);
            }
        }
        @Override public int getItemCount() { return items.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvSalon, tvEntrada;
            ViewHolder(View v) {
                super(v);
                tvNombre = v.findViewById(R.id.tv_fila_nombre);
                tvSalon = v.findViewById(R.id.tv_fila_salon);
                tvEntrada = v.findViewById(R.id.tv_fila_entrada);
            }
        }
    }
}
