package com.example.ieperuanosuizoapp.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;
    private static Context appContext = null;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            // Logging interceptor para debug
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Auth interceptor para agregar el token JWT
            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();
                    
                    // Obtener el token guardado
                    String token = null;
                    if (appContext != null) {
                        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                        token = prefs.getString("user_token", null);
                    }
                    
                    // Si hay token, agregarlo al header
                    if (token != null && !token.isEmpty()) {
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(newRequest);
                    }
                    
                    return chain.proceed(originalRequest);
                }
            };

            // Interceptor para manejar 401 (token expirado)
            Interceptor responseInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Response response = chain.proceed(chain.request());
                    
                    if (response.code() == 401 && appContext != null) {
                        SharedPreferences prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                        String token = prefs.getString("user_token", null);
                        if (token != null && !token.isEmpty()) {
                            prefs.edit().clear().apply();
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Intent intent = new Intent(appContext, com.example.ieperuanosuizoapp.AuthLogin.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                appContext.startActivity(intent);
                            });
                        }
                    }
                    
                    return response;
                }
            };

            // Cliente HTTP con timeouts e interceptors
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(responseInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build();

            // Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}
