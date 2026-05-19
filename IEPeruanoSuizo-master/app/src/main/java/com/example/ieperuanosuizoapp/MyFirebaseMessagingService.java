package com.example.ieperuanosuizoapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "asistencia_channel";
    private static final String CHANNEL_NAME = "Notificaciones de Asistencia";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Mensaje recibido de: " + remoteMessage.getFrom());

        // Verificar si el mensaje contiene notificación
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            Log.d(TAG, "Título: " + title);
            Log.d(TAG, "Cuerpo: " + body);
            
            // Mostrar notificación
            sendNotification(title, body, remoteMessage.getData());
        }

        // Verificar si el mensaje contiene datos
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Datos del mensaje: " + remoteMessage.getData());
            
            String tipo = remoteMessage.getData().get("tipo");
            Log.d(TAG, "Tipo de notificación: " + tipo);
            
            // Aquí puedes manejar diferentes tipos de notificaciones
            handleNotificationData(tipo, remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token FCM: " + token);
        
        // Enviar el token al servidor
        sendTokenToServer(token);
    }

    /**
     * Manejar datos de notificación según el tipo
     */
    private void handleNotificationData(String tipo, java.util.Map<String, String> data) {
        if (tipo == null) return;

        switch (tipo) {
            case "asistencia":
                Log.d(TAG, "Notificación de asistencia recibida");
                // Aquí puedes actualizar la UI si la app está abierta
                break;
                
            case "comunicado":
                Log.d(TAG, "Notificación de comunicado recibida");
                break;
                
            case "calificacion":
                Log.d(TAG, "Notificación de calificación recibida");
                break;
                
            default:
                Log.d(TAG, "Tipo de notificación desconocido: " + tipo);
        }
    }

    /**
     * Enviar token al servidor
     */
    private void sendTokenToServer(String token) {
        // Guardar el token localmente
        getSharedPreferences("FCM", MODE_PRIVATE)
                .edit()
                .putString("token", token)
                .apply();
        
        Log.d(TAG, "Token guardado localmente");
        
        // El token se enviará al servidor cuando el usuario inicie sesión
        // Ver AuthLogin.java para la implementación
    }

    /**
     * Crear y mostrar notificación
     */
    private void sendNotification(String title, String messageBody, java.util.Map<String, String> data) {
        // Intent para abrir la app al hacer clic en la notificación
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Agregar datos extras al intent
        if (data != null) {
            for (java.util.Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Sonido de notificación
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Construir notificación
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Crear canal de notificación para Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones de asistencia, comunicados y calificaciones");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Mostrar notificación
        notificationManager.notify(0, notificationBuilder.build());
    }
}
