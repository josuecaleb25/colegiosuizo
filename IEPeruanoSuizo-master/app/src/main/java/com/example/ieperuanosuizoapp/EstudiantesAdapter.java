package com.example.ieperuanosuizoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ieperuanosuizoapp.api.models.Estudiante;
import java.util.ArrayList;
import java.util.List;

public class EstudiantesAdapter extends RecyclerView.Adapter<EstudiantesAdapter.ViewHolder> {

    public static final int VIEW_TYPE_LIST = 0;
    public static final int VIEW_TYPE_GRID = 1;

    public interface OnEstudianteClickListener {
        void onEstudianteClick(Estudiante estudiante);
    }

    private List<Estudiante> mValues = new ArrayList<>();
    private int mCurrentViewType = VIEW_TYPE_LIST;
    private OnEstudianteClickListener mListener;

    public void setOnEstudianteClickListener(OnEstudianteClickListener listener) {
        this.mListener = listener;
    }

    public void setViewType(int viewType) {
        this.mCurrentViewType = viewType;
        notifyDataSetChanged();
    }

    public void setEstudiantes(List<Estudiante> items) {
        this.mValues = items;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return mCurrentViewType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == VIEW_TYPE_LIST) ? R.layout.item_estudiante_lista : R.layout.item_estudiante_cuadricula;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Estudiante item = mValues.get(position);
        holder.mNombreView.setText(item.getNombre());
        
        boolean esUsted = item.isEsUsted();

        if (mCurrentViewType == VIEW_TYPE_LIST) {
            if (esUsted) {
                holder.mLabelUsted.setVisibility(View.VISIBLE);
                holder.mChevron.setVisibility(View.GONE);
            } else {
                holder.mLabelUsted.setVisibility(View.GONE);
                holder.mChevron.setVisibility(View.VISIBLE);
            }
        }

        if (esUsted) {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
        } else {
            holder.itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onEstudianteClick(item);
                }
            });
            holder.itemView.setClickable(true);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mNombreView;
        public final ImageView mFotoView;
        public TextView mLabelUsted;
        public ImageView mChevron;

        public ViewHolder(View view) {
            super(view);
            mNombreView = view.findViewById(R.id.tv_nombre);
            mFotoView = view.findViewById(R.id.iv_foto);
            mLabelUsted = view.findViewById(R.id.tv_label_usted);
            mChevron = view.findViewById(R.id.iv_chevron);
        }
    }
}
