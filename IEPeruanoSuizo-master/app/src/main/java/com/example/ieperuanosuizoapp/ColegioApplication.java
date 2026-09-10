package com.example.ieperuanosuizoapp;

import android.app.Application;

import com.example.ieperuanosuizoapp.api.RetrofitClient;

public class ColegioApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
}
