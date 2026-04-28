package com.example.fishertech;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        List<NotifModel> list = new ArrayList<>();
        list.add(new NotifModel("Gale Warning", "Bawal muna pumalaot ang maliliit na sasakyang pandagat dahil sa sama ng panahon.", "10 mins ago"));
        list.add(new NotifModel("Report Update", "Ang iyong ulat tungkol sa ilegal na pangingisda ay natanggap na ng BFAR.", "1 hour ago"));
        list.add(new NotifModel("BFAR Announcement", "Mayroong libreng fingerlings distribution sa darating na Lunes.", "3 hours ago"));
        list.add(new NotifModel("Weather Update", "Maayos ang kondisyon ng dagat sa Bacoor Bay bukas ng umaga.", "5 hours ago"));

        rvNotifications.setAdapter(new NotifAdapter(list));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    static class NotifModel {
        String title, message, time;
        NotifModel(String t, String m, String tm) {
            this.title = t;
            this.message = m;
            this.time = tm;
        }
    }

    class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        private final List<NotifModel> data;

        NotifAdapter(List<NotifModel> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification, p, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int p) {
            NotifModel model = data.get(p);
            h.t.setText(model.title);
            h.m.setText(model.message);
            h.tm.setText(model.time);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView t, m, tm;
            VH(View v) {
                super(v);
                t = v.findViewById(R.id.tvNotifTitle);
                m = v.findViewById(R.id.tvNotifMessage);
                tm = v.findViewById(R.id.tvNotifTime);
            }
        }
    }
}