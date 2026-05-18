package com.example.ieperuanosuizoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ContainerEstudiantesFragment extends Fragment {

    private static final String ARG_CURSO_ID = "curso_id";
    private static final String ARG_SALON = "salon";

    public static ContainerEstudiantesFragment newInstance(String cursoId, String salon) {
        ContainerEstudiantesFragment fragment = new ContainerEstudiantesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CURSO_ID, cursoId);
        args.putString(ARG_SALON, salon);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_container_estudiantes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            String cursoId = getArguments() != null ? getArguments().getString(ARG_CURSO_ID) : null;
            String salon = getArguments() != null ? getArguments().getString(ARG_SALON) : null;
            
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.container_estudiantes_root, EstudiantesFragment.newInstance(cursoId, salon))
                    .commit();
        }
    }

    public void navegarADetalle(String nombreEstudiante) {
        getChildFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.container_estudiantes_root, DetalleCalificacionFragment.newInstance(nombreEstudiante))
                .addToBackStack(null)
                .commit();
    }
}
