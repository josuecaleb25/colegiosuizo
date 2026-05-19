package com.example.ieperuanosuizoapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.ieperuanosuizoapp.api.models.AsistenciaAlumno;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Confirmaciones de asistencia guardadas en el dispositivo (prueba / sin endpoint dedicado).
 */
public final class AsistenciaLocalStore {

    private static final String PREFS = "asistencia_local_confirmadas";
    private static final String KEY_MAP = "mapa_por_fecha";

    private AsistenciaLocalStore() {
    }

    public static void guardar(Context ctx, String fechaIso, List<AsistenciaAlumno> alumnos) {
        Map<String, List<AsistenciaAlumno>> map = cargarTodas(ctx);
        map.put(fechaIso, new ArrayList<>(alumnos));
        String json = new Gson().toJson(map);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MAP, json).apply();
    }

    public static void eliminar(Context ctx, String fechaIso) {
        Map<String, List<AsistenciaAlumno>> map = cargarTodas(ctx);
        map.remove(fechaIso);
        String json = new Gson().toJson(map);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MAP, json).apply();
    }

    public static void limpiarTodo(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static Map<String, List<AsistenciaAlumno>> cargarTodas(Context ctx) {
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MAP, null);
        if (json == null || json.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Type type = new TypeToken<Map<String, List<AsistenciaAlumno>>>() {
        }.getType();
        Map<String, List<AsistenciaAlumno>> m = new Gson().fromJson(json, type);
        return m != null ? m : new LinkedHashMap<>();
    }
}
