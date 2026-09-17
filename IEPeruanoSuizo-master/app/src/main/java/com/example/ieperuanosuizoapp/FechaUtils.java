package com.example.ieperuanosuizoapp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FechaUtils {
    private static final TimeZone LIMA = TimeZone.getTimeZone("America/Lima");
    private static final Pattern ISO_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?$");

    private FechaUtils() { }

    public static String todayKey() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(LIMA);
        return format.format(new Date());
    }

    public static String dayKey(String value) {
        Date date = parse(value);
        if (date == null) return "";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(LIMA);
        return format.format(date);
    }

    public static String formatHour(String value) {
        Date date = parse(value);
        if (date == null) return "--:--";
        SimpleDateFormat format = new SimpleDateFormat("h:mm a", new Locale("es", "PE"));
        format.setTimeZone(LIMA);
        return format.format(date).toLowerCase(new Locale("es", "PE"));
    }

    public static String formatDateLabel(String value) {
        Date date = parse(value);
        if (date == null) return "Fecha desconocida";

        Calendar event = Calendar.getInstance(LIMA);
        event.setTime(date);
        Calendar today = Calendar.getInstance(LIMA);
        Calendar yesterday = Calendar.getInstance(LIMA);
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        String month = monthName(event.get(Calendar.MONTH));
        if (sameDay(event, today)) return "Hoy, " + event.get(Calendar.DAY_OF_MONTH) + " de " + month;
        if (sameDay(event, yesterday)) return "Ayer, " + event.get(Calendar.DAY_OF_MONTH) + " de " + month;

        SimpleDateFormat format = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"));
        format.setTimeZone(LIMA);
        String result = format.format(date);
        return result.substring(0, 1).toUpperCase(new Locale("es", "ES")) + result.substring(1);
    }

    private static Date parse(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            Matcher matcher = ISO_PATTERN.matcher(value.trim());
            if (matcher.matches()) {
                String offset = matcher.group(2);
                if (offset == null || offset.isEmpty() || "Z".equals(offset)) offset = "+00:00";
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                format.setLenient(false);
                return format.parse(matcher.group(1) + offset);
            }

            SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            dateOnly.setLenient(false);
            dateOnly.setTimeZone(LIMA);
            return dateOnly.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private static String monthName(int month) {
        String[] months = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        return months[month];
    }
}
