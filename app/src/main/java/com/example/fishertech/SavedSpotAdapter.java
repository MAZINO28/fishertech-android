package com.example.fishertech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SavedSpotAdapter extends RecyclerView.Adapter<SavedSpotAdapter.SpotViewHolder> {

    public interface OnDeleteClickListener {
        void onDelete(SavedSpot spot);
    }

    private List<SavedSpot> spotList;
    private OnDeleteClickListener deleteListener;

    public SavedSpotAdapter(List<SavedSpot> spotList, OnDeleteClickListener listener) {
        this.spotList = spotList;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public SpotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_spot, parent, false);
        return new SpotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpotViewHolder holder, int position) {
        SavedSpot spot = spotList.get(position);

        holder.tvSpotName.setText(spot.name);
        holder.tvSpotCoords.setText(String.format(Locale.getDefault(), "%.4f° N,  %.4f° E", spot.latitude, spot.longitude));
        holder.tvSpotNotes.setText(spot.notes != null ? spot.notes : "");

        // 🛠️ Proteksyon para laging may "Buoy X" o "Custom Spot" na tag na lalabas
        if (spot.assignedBuoy != null && !spot.assignedBuoy.trim().isEmpty()) {
            holder.tvSpotBuoy.setText(spot.assignedBuoy.trim());
            holder.tvSpotBuoy.setVisibility(View.VISIBLE);
        } else {
            holder.tvSpotBuoy.setText("Custom Spot");
            holder.tvSpotBuoy.setVisibility(View.VISIBLE);
        }

        if (spot.createdAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.tvSpotDate.setText(sdf.format(new Date(spot.createdAt)));
        }

        holder.btnDeleteSpot.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(spot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return spotList.size();
    }

    public static class SpotViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpotName, tvSpotCoords, tvSpotNotes, tvSpotDate, tvSpotBuoy;
        ImageButton btnDeleteSpot;

        public SpotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSpotName = itemView.findViewById(R.id.tvSpotName);
            tvSpotCoords = itemView.findViewById(R.id.tvSpotCoords);
            tvSpotNotes = itemView.findViewById(R.id.tvSpotNotes);
            tvSpotDate = itemView.findViewById(R.id.tvSpotDate);
            btnDeleteSpot = itemView.findViewById(R.id.btnDeleteSpot);
            tvSpotBuoy = itemView.findViewById(R.id.tvSpotBuoy);
        }
    }
}
