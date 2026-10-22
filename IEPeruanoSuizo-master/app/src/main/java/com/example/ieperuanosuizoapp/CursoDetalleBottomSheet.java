package com.example.ieperuanosuizoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CursoDetalleBottomSheet extends BottomSheetDialogFragment {

    private String nombreCurso;
    private String nombreProfesor;
    private String salon;

    public static CursoDetalleBottomSheet newInstance(String curso, String profesor, String salon) {
        CursoDetalleBottomSheet fragment = new CursoDetalleBottomSheet();
        Bundle args = new Bundle();
        args.putString("curso", curso);
        args.putString("profesor", profesor);
        args.putString("salon", salon);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);
        if (getArguments() != null) {
            nombreCurso = getArguments().getString("curso");
            nombreProfesor = getArguments().getString("profesor");
            salon = getArguments().getString("salon");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_curso_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.tv_curso_nombre)).setText(nombreCurso);
        ((TextView) view.findViewById(R.id.tv_curso_profesor)).setText("Profesor: " + nombreProfesor);
        ((TextView) view.findViewById(R.id.tv_curso_salon)).setText("Salón: " + salon);
    }
}
