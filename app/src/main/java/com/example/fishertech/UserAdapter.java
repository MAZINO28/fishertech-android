package com.example.fishertech;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    Context context;
    ArrayList<UserModel> userList;
    ArrayList<String> userKeys;

    public UserAdapter(Context context, ArrayList<UserModel> userList, ArrayList<String> userKeys) {
        this.context = context;
        this.userList = userList;
        this.userKeys = userKeys;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);
        String userId = userKeys.get(position);

        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());
        holder.tvRole.setText("Role: " + user.getRole());

        holder.itemView.setOnClickListener(v -> showUserDetailsDialog(user));

        holder.btnDelete.setText("Delete User");
        holder.btnDelete.setBackgroundColor(Color.parseColor("#D32F2F"));

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Account")
                    .setMessage("Sigurado ka bang gusto mong burahin si " + user.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        FirebaseDatabase.getInstance()
                                .getReference("FisherTech")
                                .child("Users")
                                .child(userId)
                                .removeValue()
                                .addOnSuccessListener(unused -> Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showUserDetailsDialog(UserModel user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_user_details, null);

        TextView detName = view.findViewById(R.id.detName);
        TextView detEmail = view.findViewById(R.id.detEmail);
        TextView detPhone = view.findViewById(R.id.detPhone);
        TextView detLocation = view.findViewById(R.id.detLocation);
        TextView detBoat = view.findViewById(R.id.detBoat);
        TextView detRole = view.findViewById(R.id.detRole);
        TextView detLastLogin = view.findViewById(R.id.detLastLogin); // Idinagdag

        detName.setText("Pangalan: " + user.getName());
        detEmail.setText("Email: " + user.getEmail());
        detPhone.setText("Numero ng Telepono: " + (user.getPhone() != null ? user.getPhone() : "N/A"));
        detLocation.setText("Lokasyon: " + (user.getLocation() != null ? user.getLocation() : "N/A"));
        detBoat.setText("Numero ng Bangka: " + (user.getBoat_number() != null ? user.getBoat_number() : "N/A"));
        detRole.setText("Role: " + user.getRole());
        detLastLogin.setText("Last Online: " + formatTimestamp(user.getLast_login()));

        builder.setView(view);
        builder.setPositiveButton("Isara", null);
        builder.show();
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp == 0) return "Offline";

        long diff = System.currentTimeMillis() - timestamp;
        // 600,000 milliseconds = 10 minutes
        if (diff < 600000) {
            return "Active (Online just now)";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() { return userList.size(); }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole;
        Button btnDelete;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}