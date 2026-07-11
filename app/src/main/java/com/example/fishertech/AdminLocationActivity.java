package com.example.fishertech;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminLocationActivity extends AppCompatActivity {


    Toolbar toolbar;

    BottomNavigationView bottomNav;

    TextView tvLatitude, tvLongitude, tvAlertBanner, tvSpotsCount;

    ImageView notification, userLocationPin;

    LinearLayout alertBannerLayout;


// ✅ Single action button + its container

    LinearLayout actionButtonsLayout;

    Button btnSaveSpotAction;


    RecyclerView rvSavedSpots;

    FrameLayout mapFrame;

    ImageView mapImage;


    LinearLayout buoyContainer1, buoyContainer2, buoyContainer3;

    ImageView pinBuoy1, pinBuoy2, pinBuoy3;


    private DatabaseReference mDatabase;

    private FirebaseAuth mAuth;


    private List<SavedSpot> savedSpotList = new ArrayList<>();

    private AdminSavedSpotAdapter adminSavedSpotAdapter;


    private double selectedLat = 0;

    private double selectedLng = 0;

    private boolean hasSelectedLocation = false;


    private double buoy1Lat = 14.4520, buoy1Lng = 120.9340;

    private double buoy2Lat = 14.4506, buoy2Lng = 120.9380;

    private double buoy3Lat = 14.4490, buoy3Lng = 120.9420;


    private static final int MAX_SAVED_SPOTS = 3;


    private static final double LAT_MAX = 14.4560;

    private static final double LAT_MIN = 14.4450;

    private static final double LNG_MIN = 120.9300;

    private static final double LNG_MAX = 120.9450;


    @Override

    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_location);


        mAuth = FirebaseAuth.getInstance();

        mDatabase = FirebaseDatabase.getInstance().getReference().child("FisherTech");


        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)

            getSupportActionBar().setDisplayShowTitleEnabled(false);


        tvLatitude = findViewById(R.id.tvLatitude);

        tvLongitude = findViewById(R.id.tvLongitude);

        tvSpotsCount = findViewById(R.id.tvSpotsCount);

        notification = findViewById(R.id.notification);

        alertBannerLayout = findViewById(R.id.alertBannerLayout);

        tvAlertBanner = findViewById(R.id.tvAlertBanner);

        rvSavedSpots = findViewById(R.id.rvSavedSpots);

        userLocationPin = findViewById(R.id.userLocationPin);

        mapFrame = findViewById(R.id.mapFrame);

        mapImage = findViewById(R.id.mapPlaceholder);

        actionButtonsLayout = findViewById(R.id.actionButtonsLayout);

        btnSaveSpotAction = findViewById(R.id.btnSaveSpotAction);


        buoyContainer1 = findViewById(R.id.buoyContainer1);

        buoyContainer2 = findViewById(R.id.buoyContainer2);

        buoyContainer3 = findViewById(R.id.buoyContainer3);

        pinBuoy1 = findViewById(R.id.pinBuoy1);

        pinBuoy2 = findViewById(R.id.pinBuoy2);

        pinBuoy3 = findViewById(R.id.pinBuoy3);


        if (notification != null)

            notification.setOnClickListener(v ->

                    startActivity(new Intent(this, NotificationActivity.class)));


        adminSavedSpotAdapter = new AdminSavedSpotAdapter(savedSpotList, this::deleteSavedSpot);

        rvSavedSpots.setLayoutManager(new LinearLayoutManager(this));

        rvSavedSpots.setAdapter(adminSavedSpotAdapter);


        listenToBuoysFromFirebase();

        setupMapTapToPlace();


        pinBuoy1.setOnClickListener(v -> showBuoyInfo(1));

        pinBuoy2.setOnClickListener(v -> showBuoyInfo(2));

        pinBuoy3.setOnClickListener(v -> showBuoyInfo(3));


// ✅ Single button → opens combined dropdown dialog

        btnSaveSpotAction.setOnClickListener(v -> {

            if (!hasSelectedLocation) {

                Toast.makeText(this, "Mag-tap muna sa mapa.", Toast.LENGTH_SHORT).show();

                return;

            }

            checkLimitAndSave();

        });


        loadSavedSpots();

        checkBuoyAlerts();

        setupNavigation();

    }


// ─────────────────────────────────────────────

// TAP MAP → PLACE SINGLE MARKER

// ─────────────────────────────────────────────


    private void setupMapTapToPlace() {

        mapImage.setOnTouchListener((v, event) -> {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                float tapX = event.getX();

                float tapY = event.getY();


                userLocationPin.setVisibility(View.VISIBLE);

                userLocationPin.post(() -> {

                    userLocationPin.setX(tapX - (userLocationPin.getWidth() / 2f));

                    userLocationPin.setY(tapY - userLocationPin.getHeight());

                });


                int imgWidth = mapImage.getWidth();

                int imgHeight = mapImage.getHeight();

                float correctedY = tapY - (userLocationPin.getHeight() / 2f);


                selectedLat = mapYToLat(correctedY, imgHeight);

                selectedLng = mapXToLng(tapX, imgWidth);

                hasSelectedLocation = true;


                tvLatitude.setText(String.format("%.4f° N", selectedLat));

                tvLongitude.setText(String.format("%.4f° E", selectedLng));


// ✅ Show the single save button

                actionButtonsLayout.setVisibility(View.VISIBLE);

            }

            return true;

        });

    }


// ─────────────────────────────────────────────

// COMBINED SAVE DIALOG (ALWAYS VISIBLE DROPDOWN)

// ─────────────────────────────────────────────


    private void checkLimitAndSave() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) return;


        mDatabase.child("saved_spots").child(user.getUid())

                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override

                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.getChildrenCount() >= MAX_SAVED_SPOTS) {

                            Toast.makeText(AdminLocationActivity.this,

                                    "Maximum na ang iyong saved spots (3 lang).\nBurahin muna ang isa.",

                                    Toast.LENGTH_LONG).show();

                        } else {

                            showCombinedSaveDialog();

                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }

                });

    }


    private void showCombinedSaveDialog() {

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_spot, null);


// Get views

        TextView tvCoords = dialogView.findViewById(R.id.tvSelectedCoords);

        EditText etSpotName = dialogView.findViewById(R.id.etSpotName);

        EditText etSpotNotes = dialogView.findViewById(R.id.etSpotNotes);


// ✅ Dropdown Spinner Setup (No checkbox needed)

        Spinner spinnerBuoys = dialogView.findViewById(R.id.spinnerBuoys);

        Button btnSave = dialogView.findViewById(R.id.btnSaveSpot);

        Button btnCancel = dialogView.findViewById(R.id.btnCancelSpot);


// Show tapped coordinates

        if (tvCoords != null)

            tvCoords.setText(String.format("📍 %.4f° N, %.4f° E", selectedLat, selectedLng));


// ✅ Direktang i-populate ang choices sa Dropdown

        String[] buoyOptions = {"🟡 Buoy 1", "🟡 Buoy 2", "🟡 Buoy 3"};

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(

                this, android.R.layout.simple_spinner_item, buoyOptions);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerBuoys.setAdapter(adapter);


        AlertDialog dialog = new AlertDialog.Builder(this)

                .setView(dialogView)

                .create();

        if (dialog.getWindow() != null)

            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);


        btnSave.setOnClickListener(v -> {

            String name = etSpotName.getText().toString().trim();

            String notes = etSpotNotes.getText().toString().trim();


            if (name.isEmpty()) {

                etSpotName.setError("Lagyan ng pangalan");

                return;

            }


// ✅ KUNIN ANG POSISYON: Para makasiguro na malinis na "Buoy X" ang masesave sa DB

            int selectedPosition = spinnerBuoys.getSelectedItemPosition();

            int buoyNum = selectedPosition + 1; // Magiging 1, 2, o 3

            String cleanBuoyName = "Buoy " + buoyNum;


// 1. ✅ I-save ang fishing spot kasama ang nalinis na pangalan ng boya

            saveCurrentLocation(name, notes.isEmpty() ? "Minarkahan mula sa app" : notes, cleanBuoyName);


// 2. ✅ Awtomatikong ilipat kung anong boya ang pinili sa dropdown

            moveBuoyToSelectedSpot(buoyNum);


            dialog.dismiss();

        });


        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

    }


// ─────────────────────────────────────────────

// SAVE FISHING SPOT TO FIREBASE

// ─────────────────────────────────────────────


    private void saveCurrentLocation(String spotName, String notes, String assignedBuoy) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Gamitin ang "all_saved_spots" para sa lahat
        String spotId = mDatabase.child("all_saved_spots").push().getKey();

        Map<String, Object> spotData = new HashMap<>();
        spotData.put("name", spotName);
        spotData.put("latitude", selectedLat);
        spotData.put("longitude", selectedLng);
        spotData.put("created_at", System.currentTimeMillis());
        spotData.put("notes", notes);
        spotData.put("assigned_buoy", assignedBuoy);
        spotData.put("owner", user.getUid()); // I-save kung kanino galing

        if (spotId != null) {
            mDatabase.child("all_saved_spots").child(spotId).setValue(spotData)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Na-save: " + spotName, Toast.LENGTH_SHORT).show();
                        clearMarker();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }


// ─────────────────────────────────────────────

// MOVE BUOY TO SELECTED SPOT

// ─────────────────────────────────────────────


    private void moveBuoyToSelectedSpot(int buoyNum) {

        Map<String, Object> update = new HashMap<>();

        update.put("latitude", selectedLat);

        update.put("longitude", selectedLng);

        update.put("updated_at", System.currentTimeMillis());


        mDatabase.child("buoys").child("buoy_" + buoyNum).updateChildren(update)

                .addOnSuccessListener(aVoid ->

                        Toast.makeText(this,

                                "Buoy " + buoyNum + " ay nailipat na!",

                                Toast.LENGTH_SHORT).show())

                .addOnFailureListener(e ->

                        Toast.makeText(this, "Error sa buoy: " + e.getMessage(),

                                Toast.LENGTH_SHORT).show());

    }


// ─────────────────────────────────────────────

// FIREBASE BUOY SYNC

// ─────────────────────────────────────────────


    private void listenToBuoysFromFirebase() {

        mDatabase.child("buoys").addValueEventListener(new ValueEventListener() {

            @Override

            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {

                    initializeDefaultBuoysInFirebase();

                    return;

                }

                if (snapshot.hasChild("buoy_1")) {

                    Double lat = snapshot.child("buoy_1").child("latitude").getValue(Double.class);

                    Double lng = snapshot.child("buoy_1").child("longitude").getValue(Double.class);

                    if (lat != null) buoy1Lat = lat;

                    if (lng != null) buoy1Lng = lng;

                }

                if (snapshot.hasChild("buoy_2")) {

                    Double lat = snapshot.child("buoy_2").child("latitude").getValue(Double.class);

                    Double lng = snapshot.child("buoy_2").child("longitude").getValue(Double.class);

                    if (lat != null) buoy2Lat = lat;

                    if (lng != null) buoy2Lng = lng;

                }

                if (snapshot.hasChild("buoy_3")) {

                    Double lat = snapshot.child("buoy_3").child("latitude").getValue(Double.class);

                    Double lng = snapshot.child("buoy_3").child("longitude").getValue(Double.class);

                    if (lat != null) buoy3Lat = lat;

                    if (lng != null) buoy3Lng = lng;

                }

                mapFrame.post(() -> {

                    int w = mapFrame.getWidth();

                    int h = mapFrame.getHeight();

                    if (w > 0 && h > 0) {

                        setContainerPosition(buoyContainer1, latToY(buoy1Lat, h), lngToX(buoy1Lng, w));

                        setContainerPosition(buoyContainer2, latToY(buoy2Lat, h), lngToX(buoy2Lng, w));

                        setContainerPosition(buoyContainer3, latToY(buoy3Lat, h), lngToX(buoy3Lng, w));

                    }

                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }

        });

    }


    private void initializeDefaultBuoysInFirebase() {

        for (int i = 1; i <= 3; i++) {

            Map<String, Object> def = new HashMap<>();

            def.put("latitude", i == 1 ? 14.4520 : (i == 2 ? 14.4506 : 14.4490));

            def.put("longitude", i == 1 ? 120.9340 : (i == 2 ? 120.9380 : 120.9420));

            def.put("updated_at", System.currentTimeMillis());

            mDatabase.child("buoys").child("buoy_" + i).setValue(def);

        }

    }


// ─────────────────────────────────────────────

// BUOY INFO DIALOG

// ─────────────────────────────────────────────


    private void showBuoyInfo(int buoyNum) {

        double lat = buoyNum == 1 ? buoy1Lat : (buoyNum == 2 ? buoy2Lat : buoy3Lat);

        double lng = buoyNum == 1 ? buoy1Lng : (buoyNum == 2 ? buoy2Lng : buoy3Lng);


        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_buoy_info, null);


        ((TextView) dialogView.findViewById(R.id.tvBuoyName)).setText("Buoy " + buoyNum);

        ((TextView) dialogView.findViewById(R.id.tvBuoyCoords))

                .setText(String.format("%.4f° N, %.4f° E", lat, lng));

        TextView tvStatus = dialogView.findViewById(R.id.tvBuoyStatus);

        tvStatus.setText("Active");

        tvStatus.setBackgroundResource(R.drawable.badge_green);


        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        if (dialog.getWindow() != null)

            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.btnCloseBuoyInfo).setOnClickListener(v -> dialog.dismiss());

        dialog.show();

    }


// ─────────────────────────────────────────────

// BUOY ALERT BANNER

// ─────────────────────────────────────────────


    private void checkBuoyAlerts() {

        mDatabase.child("boya_images").addValueEventListener(new ValueEventListener() {

            @Override

            public void onDataChange(@NonNull DataSnapshot snapshot) {

                boolean hasAlert = false;

                String alertMessage = "";

                for (DataSnapshot data : snapshot.getChildren()) {

                    String status = data.child("status").getValue(String.class);

                    String location = data.child("location").getValue(String.class);

                    if ("suspicious".equals(status) || "stolen".equals(status)) {

                        hasAlert = true;

                        alertMessage = "⚠️ " + location + " ay may hindi awtorisadong galaw!";

                        break;

                    }

                }

                if (alertBannerLayout != null) {

                    alertBannerLayout.setVisibility(hasAlert ? View.VISIBLE : View.GONE);

                    if (hasAlert) tvAlertBanner.setText(alertMessage);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }

        });

    }


// ─────────────────────────────────────────────

// SAVED SPOTS (LOAD ENGINE)

// ─────────────────────────────────────────────


    private void loadSavedSpots() {
        // 1. Inalis ang check para sa 'user == null' kung gusto mong makita ng lahat ang spots,
        //    kahit hindi naka-login (o kahit naka-login man).

        // 2. Binago ang path mula "saved_spots" patungong "all_saved_spots"
        mDatabase.child("all_saved_spots").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                savedSpotList.clear();

                // 3. I-loop ang mga items sa loob ng "all_saved_spots"
                for (DataSnapshot data : snapshot.getChildren()) {
                    String spotId = data.getKey();
                    String name = data.child("name").getValue(String.class);
                    String notes = data.child("notes").getValue(String.class);
                    Double lat = data.child("latitude").getValue(Double.class);
                    Double lng = data.child("longitude").getValue(Double.class);
                    Long createdAt = data.child("created_at").getValue(Long.class);
                    String assignedBuoy = data.child("assigned_buoy").getValue(String.class);

                    if (name != null && lat != null && lng != null) {
                        savedSpotList.add(new SavedSpot(spotId, name, notes, lat, lng, createdAt, assignedBuoy));
                    }
                }

                // 4. I-update ang UI
                adminSavedSpotAdapter.notifyDataSetChanged();

                if (tvSpotsCount != null) {
                    tvSpotsCount.setText(savedSpotList.size() + " total shared spots");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminLocationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void deleteSavedSpot(SavedSpot spot) {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) return;

        new AlertDialog.Builder(this)

                .setTitle("Burahin ang Spot")

                .setMessage("Sigurado ka bang gusto mong burahin ang \"" + spot.name + "\"?")

                .setPositiveButton("Oo", (d, w) ->

                        mDatabase.child("all_saved_spots").child(spot.spotId).removeValue()

                                .addOnSuccessListener(a ->

                                        Toast.makeText(this, "Na-delete na.", Toast.LENGTH_SHORT).show()))

                .setNegativeButton("Hindi", null).show();

    }


// ─────────────────────────────────────────────

// HELPERS

// ─────────────────────────────────────────────


    private void clearMarker() {

        hasSelectedLocation = false;

        userLocationPin.setVisibility(View.INVISIBLE);

        actionButtonsLayout.setVisibility(View.GONE);

        tvLatitude.setText("14.4506° N");

        tvLongitude.setText("120.9358° E");

    }


    private double mapYToLat(float y, int h) {

        return LAT_MAX - ((y / h) * (LAT_MAX - LAT_MIN));

    }


    private double mapXToLng(float x, int w) {

        return LNG_MIN + ((x / w) * (LNG_MAX - LNG_MIN));

    }


    private float latToY(double lat, int h) {

        return (float) ((LAT_MAX - lat) / (LAT_MAX - LAT_MIN) * h);

    }


    private float lngToX(double lng, int w) {

        return (float) ((lng - LNG_MIN) / (LNG_MAX - LNG_MIN) * w);

    }


    private void setContainerPosition(LinearLayout container, float y, float x) {

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) container.getLayoutParams();

        int halfW = container.getWidth() / 2;

        int halfH = container.getHeight() / 2;

        params.leftMargin = (int) x - halfW;

        params.topMargin = (int) y - halfH;

        container.setLayoutParams(params);

    }


// ─────────────────────────────────────────────

// NAVIGATION

// ─────────────────────────────────────────────


    private void setupNavigation() {

        bottomNav = findViewById(R.id.bottomNav);

        if (bottomNav == null) return;

        bottomNav.setSelectedItemId(R.id.nav_location);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_location) return true;

            Intent intent = null;

            if (id == R.id.nav_home) intent = new Intent(this, DashboardAdminActivity.class);

            else if (id == R.id.nav_chat) intent = new Intent(this, AdminChatActivity.class);

            else if (id == R.id.nav_report) intent = new Intent(this, AdminReportsActivity.class);

            else if (id == R.id.nav_profile) intent = new Intent(this, AdminProfileActivity.class);

            if (intent != null) {

                startActivity(intent);

                overridePendingTransition(0, 0);

                finish();

                return true;

            }

            return false;

        });

    }
}
