package com.example.ieperuanosuizoapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class NotificationPermissionHelper {

    private static final String TAG = "NotificationPermission";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    /**
     * Verificar si el permiso de notificaciones está otorgado
     */
    public static boolean hasNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        // Para versiones anteriores a Android 13, no se necesita permiso
        return true;
    }

    /**
     * Solicitar permiso de notificaciones
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                Log.d(TAG, "Solicitando permiso de notificaciones");
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE
                );
            } else {
                Log.d(TAG, "Permiso de notificaciones ya otorgado");
            }
        }
    }

    /**
     * Manejar resultado de solicitud de permiso
     */
    public static void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso de notificaciones otorgado");
            } else {
                Log.d(TAG, "Permiso de notificaciones denegado");
            }
        }
    }
}
