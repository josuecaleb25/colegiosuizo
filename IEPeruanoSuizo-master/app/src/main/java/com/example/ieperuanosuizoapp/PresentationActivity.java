package com.example.ieperuanosuizoapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class PresentationActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutDots;
    private Button btnNext;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);

        layoutDots = findViewById(R.id.layoutDots);
        btnNext = findViewById(R.id.btnNext);
        viewPager = findViewById(R.id.viewPager);
        TextView tvSkip = findViewById(R.id.tvSkip);

        setupOnboardingItems();

        viewPager.setAdapter(onboardingAdapter);
        // Activar por aove sea mas conveniente (deslizamiento)
        viewPager.setUserInputEnabled(false);
        
        setupDots();
        setCurrentDot(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentDot(position);
                if (position == onboardingAdapter.getItemCount() - 1) {
                    btnNext.setText("Empezar");
                } else {
                    btnNext.setText("Next");
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true); // True para animación suave
            } else {
                navigateToLogin();
            }
        });

        tvSkip.setOnClickListener(v -> navigateToLogin());
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        onboardingItems.add(new OnboardingItem(
                R.drawable.progressacademico,
                "Sigue tu progreso académico",
                "Consulta tus faltas acumuladas, tus notas y el registro detallado de tus clases en tiempo real para mantener tus estudios al día"
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.horarioescolar,
                "Controla tu horario escolar",
                "Lleva el registro de tus cursos, salones y horarios en un solo lugar"
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.matentealdia,
                "Mantente al Día",
                "Recibe comunicados importantes, horarios de atención a padres en la palma de tu mano"
        ));

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
    }

    private void setupDots() {
        ImageView[] dots = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(getApplicationContext());
            dots[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.dot_inactive
            ));
            dots[i].setLayoutParams(params);
            layoutDots.addView(dots[i]);
        }
    }

    private void setCurrentDot(int position) {
        int childCount = layoutDots.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutDots.getChildAt(i);
            if (i == position) {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(),
                        R.drawable.dot_active
                ));
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(),
                        R.drawable.dot_inactive
                ));
            }
        }
    }

    private void navigateToLogin() {
        startActivity(new Intent(PresentationActivity.this, AuthLogin.class));
        finish();
    }
}