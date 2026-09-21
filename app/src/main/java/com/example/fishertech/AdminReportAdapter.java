package com.example.fishertech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.ReportViewHolder> {

    private List<AdminReportsActivity.ReportPost> reportList;
    private OnActionClickListener actionClickListener;

    public interface OnActionClickListener {
        void onSaveClick(AdminReportsActivity.ReportPost post);
        void onDeleteClick(String reportId);
    }

    public AdminReportAdapter(List<AdminReportsActivity.ReportPost> reportList, OnActionClickListener listener) {
        this.reportList = reportList;
        this.actionClickListener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Gumagamit na ng bagong admin layout na may delete button
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_report_post, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        AdminReportsActivity.ReportPost post = reportList.get(position);

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

        // Save button listener
        holder.btnSaveFile.setOnClickListener(v -> {
            if (actionClickListener != null) {
                actionClickListener.onSaveClick(post);
            }
        });

        // Delete button listener
        holder.btnDeleteReport.setOnClickListener(v -> {
            if (actionClickListener != null) {
                actionClickListener.onDeleteClick(post.getReportId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {

        TextView tvUserName, tvCategory, tvDescription, tvAdditionalRemarks, tvTimestamp;
        Button btnSaveFile, btnDeleteReport;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAdditionalRemarks = itemView.findViewById(R.id.tvAdditionalRemarks);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnSaveFile = itemView.findViewById(R.id.btnSaveFile);
            btnDeleteReport = itemView.findViewById(R.id.btnDeleteReport);
        }
    }
}