package com.example.fishertech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private List<ReportActivity.ReportPost> reportList;
    private OnSaveClickListener saveClickListener;

    public interface OnSaveClickListener {
        void onSaveClick(ReportActivity.ReportPost post);
    }

    public ReportAdapter(List<ReportActivity.ReportPost> reportList, OnSaveClickListener listener) {
        this.reportList = reportList;
        this.saveClickListener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_post, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ReportActivity.ReportPost post = reportList.get(position);
        holder.tvUserName.setText(post.name);
        holder.tvCategory.setText(post.category);
        holder.tvDescription.setText(post.description);
        holder.tvTimestamp.setText(post.timestamp);

        if (post.additionalRemarks != null && !post.additionalRemarks.trim().isEmpty()) {
            holder.tvAdditionalRemarks.setVisibility(View.VISIBLE);
            holder.tvAdditionalRemarks.setText(post.additionalRemarks);
        } else {
            holder.tvAdditionalRemarks.setVisibility(View.GONE);
        }

        // Click listener para sa save button
        holder.btnSaveFile.setOnClickListener(v -> {
            if (saveClickListener != null) {
                saveClickListener.onSaveClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvCategory, tvDescription, tvAdditionalRemarks, tvTimestamp;
        Button btnSaveFile;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAdditionalRemarks = itemView.findViewById(R.id.tvAdditionalRemarks);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnSaveFile = itemView.findViewById(R.id.btnSaveFile);
        }
    }
}