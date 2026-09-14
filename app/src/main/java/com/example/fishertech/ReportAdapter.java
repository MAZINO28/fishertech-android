package com.example.fishertech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private List<ReportActivity.ReportPost> reportList;

    public ReportAdapter(List<ReportActivity.ReportPost> reportList) {
        this.reportList = reportList;
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

        // Idinagdag para ipakita ang karagdagang puna kung mayroon man
        if (post.additionalRemarks != null && !post.additionalRemarks.trim().isEmpty()) {
            holder.tvAdditionalRemarks.setVisibility(View.VISIBLE);
            holder.tvAdditionalRemarks.setText(post.additionalRemarks);
        } else {
            holder.tvAdditionalRemarks.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvCategory, tvDescription, tvAdditionalRemarks;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAdditionalRemarks = itemView.findViewById(R.id.tvAdditionalRemarks);
        }
    }
}