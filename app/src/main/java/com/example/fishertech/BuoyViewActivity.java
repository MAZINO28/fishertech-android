package com.example.fishertech;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageMetadata;

public class BuoyViewActivity extends AppCompatActivity {

    private RecyclerView rvBuoyList;
    private BuoyAdapter adapter;
    private List<BuoyModel> buoyList;
    private DatabaseReference dbRef;
    private static final String TAG = "BuoyViewDebug"; // Tag para sa Logcat

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buoy_view);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvBuoyList = findViewById(R.id.rvBuoyList);
        rvBuoyList.setLayoutManager(new LinearLayoutManager(this));

        buoyList = new ArrayList<>();
        adapter = new BuoyAdapter(buoyList, this); // Pinasa ang context
        rvBuoyList.setAdapter(adapter);

        MaterialButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> fetchDataFromFirebase());

        fetchDataFromFirebase();
    }

    private void fetchDataFromFirebase() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference listRef = storage.getReference().child("buoys");

        listRef.listAll().addOnSuccessListener(listResult -> {
            buoyList.clear();
            for (StorageReference item : listResult.getItems()) {
                item.getMetadata().addOnSuccessListener(storageMetadata -> {
                    long timeCreated = storageMetadata.getCreationTimeMillis();

                    item.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Idagdag ang item sa list
                        buoyList.add(new BuoyModel(uri.toString(), "Boya 1", timeCreated));

                        // Siguraduhing ma-notify ang adapter sa UI thread
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    });
                });
            }
        }).addOnFailureListener(e -> {
            // Pinalitan ang 'context' ng 'BuoyViewActivity.this'
            Toast.makeText(BuoyViewActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // --- MODEL CLASS ---
    public static class BuoyModel {
        public String image_url, location;
        public Long captured_at;
        public BuoyModel() {}
        public BuoyModel(String url, String loc, Long time) {
            this.image_url = url; this.location = loc; this.captured_at = time;
        }
    }

    // --- ADAPTER CLASS (Inayos para sa Glide context) ---
    private class BuoyAdapter extends RecyclerView.Adapter<BuoyAdapter.ViewHolder> {
        private List<BuoyModel> list;
        private AppCompatActivity activity;

        public BuoyAdapter(List<BuoyModel> list, AppCompatActivity activity) {
            this.list = list;
            this.activity = activity;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_buoy_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BuoyModel item = list.get(position);

            // 1. I-set ang Location
            holder.tvLocation.setText(item.location != null ? item.location : "Boya 1");

            // 2. DITO MO ILAGAY YUNG DATE/TIME LOGIC
            if (item.captured_at != null && item.captured_at > 0) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault());
                String dateString = sdf.format(new java.util.Date(item.captured_at));
                holder.tvTimestamp.setText("Petsa: " + dateString);
            } else {
                holder.tvTimestamp.setText("Petsa: N/A");
            }

            // 3. Glide: Dito pinapagana ang image loading
            Glide.with(activity)
                    .load(item.image_url)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.stat_notify_error)
                    .into(holder.ivBuoyCapture);
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivBuoyCapture;
            TextView tvLocation, tvTimestamp;
            ViewHolder(View v) {
                super(v);
                ivBuoyCapture = v.findViewById(R.id.ivBuoyCapture);
                tvLocation = v.findViewById(R.id.tvLocation);
                tvTimestamp = v.findViewById(R.id.tvTimestamp);
            }
        }
    }
}