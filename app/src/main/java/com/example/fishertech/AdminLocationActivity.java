package com.example.fishertech;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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

public class AdminLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    Toolbar toolbar;
    BottomNavigationView bottomNav;
    TextView tvLatitude, tvLongitude, tvAlertBanner, tvSpotsCount;
    LinearLayout alertBannerLayout, actionButtonsLayout;
    View btnNotification;
    Button btnSaveSpotAction;
    RecyclerView rvSavedSpots;
    private SwipeRefreshLayout swipeRefreshLayout;

    private GoogleMap mMap;
    private Marker userSelectedMarker;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    // ✅ Para sa Real-time Notification Badge Listener
    private DatabaseReference notifRef;
    private ValueEventListener badgeListener;

    private List<SavedSpot> savedSpotList = new ArrayList<>();
    private AdminSavedSpotAdapter adminSavedSpotAdapter;

    private double selectedLat = 14.401895; // Gitna ng Bacoor Bay
    private double selectedLng = 120.967415;
    private boolean hasSelectedLocation = false;

    private final Map<String, Marker> buoyMarkers = new HashMap<>();
    private final Map<String, Marker> savedSpotMarkers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_location);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("FisherTech");
        notifRef = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("notifications");

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvSpotsCount = findViewById(R.id.tvSpotsCount);
        alertBannerLayout = findViewById(R.id.alertBannerLayout);
        tvAlertBanner = findViewById(R.id.tvAlertBanner);
        rvSavedSpots = findViewById(R.id.rvSavedSpots);
        actionButtonsLayout = findViewById(R.id.actionButtonsLayout);
        btnSaveSpotAction = findViewById(R.id.btnSaveSpotAction);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // ✅ Sinusubukang hanapin ang notification gamit ang iba't ibang posibleng ID mula sa toolbar
        View toolbarView = findViewById(R.id.toolbar);
        if (toolbarView != null) {
            btnNotification = toolbarView.findViewById(R.id.btnNotificationContainer);
            if (btnNotification == null) {
                btnNotification = toolbarView.findViewById(R.id.notification);
            }
        }
        if (btnNotification == null) {
            btnNotification = findViewById(R.id.btnNotificationContainer);
            if (btnNotification == null) {
                btnNotification = findViewById(R.id.notification);
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        adminSavedSpotAdapter = new AdminSavedSpotAdapter(savedSpotList, this::deleteSavedSpot);
        rvSavedSpots.setLayoutManager(new LinearLayoutManager(this));
        rvSavedSpots.setAdapter(adminSavedSpotAdapter);

        if (btnSaveSpotAction != null) {
            btnSaveSpotAction.setOnClickListener(v -> checkLimitAndSave());
        }

        // ✅ Magbubukas ang NotificationActivity kapag pinindot ang abiso icon/container sa toolbar
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                startActivity(new Intent(AdminLocationActivity.this, NotificationActivity.class));
            });
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadSavedSpots();
                checkBuoyAlerts();
                swipeRefreshLayout.setRefreshing(false);
            });
        }

        loadSavedSpots();
        checkBuoyAlerts();
        setupNavigation();
    }

    // ✅ Simulan ang badge listener tuwing nakikita ang screen
    @Override
    protected void onStart() {
        super.onStart();
        startBadgeListener();
    }

    // ✅ Itigil ang listener kapag lumipat ng ibang screen para walang memory leak
    @Override
    protected void onStop() {
        super.onStop();
        stopBadgeListener();
    }

    // ================== UNREAD BADGE SYSTEM ==================

    private void startBadgeListener() {
        if (badgeListener != null) return;

        badgeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long lastSeen = Notificationprefs.getLastSeen(AdminLocationActivity.this);

                int unreadCount = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Long createdAt = snap.child("created_at").getValue(Long.class);
                    if (createdAt == null) continue;

                    if (createdAt <= lastSeen) continue;
                    unreadCount++;
                }

                updateBadge(unreadCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BadgeError", "Error loading unread notifications: " + error.getMessage());
            }
        };

        notifRef.addValueEventListener(badgeListener);
    }

    private void stopBadgeListener() {
        if (badgeListener != null && notifRef != null) {
            notifRef.removeEventListener(badgeListener);
        }
        badgeListener = null;
    }

    private void updateBadge(int unreadCount) {
        Toolbar toolbarView = findViewById(R.id.toolbar);
        if (toolbarView == null) return;

        TextView tvBadge = toolbarView.findViewById(R.id.tvNotificationBadge);
        if (tvBadge == null) return;

        if (unreadCount > 0) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            tvBadge.bringToFront();
        } else {
            tvBadge.setVisibility(View.GONE);
        }
    }

    // ========================================================

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Gawing Satellite View ang mapa sa admin side at ayusin ang padding
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.setPadding(0, 0, 0, 70);

        LatLng bacoorBayWater = new LatLng(14.412652, 120.982077);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bacoorBayWater, 14f));

        listenToBuoysFromFirebase();
        loadSavedSpotsMarkers();

        mMap.setOnMapClickListener(latLng -> {
            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;
            hasSelectedLocation = true;

            tvLatitude.setText(String.format("%.4f° N", selectedLat));
            tvLongitude.setText(String.format("%.4f° E", selectedLng));

            if (userSelectedMarker != null) {
                userSelectedMarker.remove();
            }
            userSelectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Piniling Spot ng Admin")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            if (actionButtonsLayout != null) {
                actionButtonsLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void listenToBuoysFromFirebase() {
        mDatabase.child("buoys").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                boolean hasFishGlobalAlert = false;
                List<String> activeFishBuoys = new ArrayList<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String buoyKey = snap.getKey();
                    Double lat = snap.child("latitude").getValue(Double.class);
                    Double lng = snap.child("longitude").getValue(Double.class);

                    Boolean fishDetected = snap.child("fish_detected").getValue(Boolean.class);
                    String catchStatus = snap.child("catch_status").getValue(String.class);

                    if (lat != null && lng != null) {
                        LatLng pos = new LatLng(lat, lng);
                        String buoyNum = buoyKey.replace("buoy_", "");
                        String buoyTitle = "Boya " + buoyNum;

                        String snippetText;
                        float markerHue;

                        if (fishDetected != null && fishDetected) {
                            snippetText = (catchStatus != null && !catchStatus.isEmpty()) ? catchStatus : "🐟 May Isda na Na-detect!";
                            markerHue = BitmapDescriptorFactory.HUE_GREEN;

                            hasFishGlobalAlert = true;
                            activeFishBuoys.add(buoyTitle);
                        } else {
                            snippetText = "Aktibong Boya";
                            markerHue = BitmapDescriptorFactory.HUE_RED;
                        }

                        if (buoyMarkers.containsKey(buoyKey)) {
                            Marker marker = buoyMarkers.get(buoyKey);
                            if (marker != null) {
                                marker.setPosition(pos);
                                marker.setTitle(buoyTitle);
                                marker.setSnippet(snippetText);
                                marker.setIcon(BitmapDescriptorFactory.defaultMarker(markerHue));
                            }
                        } else {
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title(buoyTitle)
                                    .snippet(snippetText)
                                    .icon(BitmapDescriptorFactory.defaultMarker(markerHue)));
                            buoyMarkers.put(buoyKey, marker);
                        }
                    } else {
                        if (buoyMarkers.containsKey(buoyKey)) {
                            Marker marker = buoyMarkers.get(buoyKey);
                            if (marker != null) {
                                marker.remove();
                            }
                            buoyMarkers.remove(buoyKey);
                        }
                    }
                }

                if (alertBannerLayout != null && tvAlertBanner != null) {
                    if (hasFishGlobalAlert) {
                        alertBannerLayout.setVisibility(View.VISIBLE);
                        String combinedBuoys = android.text.TextUtils.join(", ", activeFishBuoys);
                        tvAlertBanner.setText("🐟 Alerto: May nahuling isda sa " + combinedBuoys + "!");
                    } else {
                        alertBannerLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadSavedSpotsMarkers() {
        mDatabase.child("all_saved_spots").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (Marker marker : savedSpotMarkers.values()) {
                    if (marker != null) marker.remove();
                }
                savedSpotMarkers.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String spotId = data.getKey();
                    String name = data.child("name").getValue(String.class);
                    Double lat = data.child("latitude").getValue(Double.class);
                    Double lng = data.child("longitude").getValue(Double.class);

                    if (name != null && lat != null && lng != null && mMap != null) {
                        LatLng pos = new LatLng(lat, lng);
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title("Saved Spot: " + name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        if (marker != null) {
                            savedSpotMarkers.put(spotId, marker);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkLimitAndSave() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Mag-sign in muna bilang admin.", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("all_saved_spots").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showCombinedSaveDialog();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showCombinedSaveDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_spot, null);

        TextView tvCoords = dialogView.findViewById(R.id.tvSelectedCoords);
        EditText etSpotName = dialogView.findViewById(R.id.etSpotName);
        EditText etSpotNotes = dialogView.findViewById(R.id.etSpotNotes);
        Spinner spinnerBuoys = dialogView.findViewById(R.id.spinnerBuoys);
        Button btnSave = dialogView.findViewById(R.id.btnSaveSpot);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelSpot);

        if (tvCoords != null)
            tvCoords.setText(String.format("📍 %.4f° N, %.4f° E", selectedLat, selectedLng));

        String[] buoyOptions = {"🔴 Boya Spot 1", "🔴 Boya Spot 2", "🔴 Boya Spot 3"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, buoyOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerBuoys != null) spinnerBuoys.setAdapter(adapter);

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

            int selectedPosition = spinnerBuoys != null ? spinnerBuoys.getSelectedItemPosition() : 0;
            int buoyNum = selectedPosition + 1;
            String cleanBuoyName = "Boya Spot " + buoyNum;

            saveOrUpdateSpot(name, notes.isEmpty() ? "Minarkahan ng Admin" : notes, cleanBuoyName);

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveOrUpdateSpot(String spotName, String notes, String assignedBuoy) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        mDatabase.child("all_saved_spots").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingSpotId = null;

                for (DataSnapshot data : snapshot.getChildren()) {
                    String buoy = data.child("assigned_buoy").getValue(String.class);
                    if (buoy != null && buoy.equals(assignedBuoy)) {
                        existingSpotId = data.getKey();
                        break;
                    }
                }

                if (existingSpotId != null) {
                    mDatabase.child("all_saved_spots").child(existingSpotId).removeValue();
                }

                String spotId = mDatabase.child("all_saved_spots").push().getKey();
                Map<String, Object> spotData = new HashMap<>();
                spotData.put("name", spotName);
                spotData.put("latitude", selectedLat);
                spotData.put("longitude", selectedLng);
                spotData.put("created_at", System.currentTimeMillis());
                spotData.put("notes", notes);
                spotData.put("assigned_buoy", assignedBuoy);
                spotData.put("owner", user.getUid());

                if (spotId != null) {
                    mDatabase.child("all_saved_spots").child(spotId).setValue(spotData)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(AdminLocationActivity.this, "Na-update ang spot para sa " + assignedBuoy, Toast.LENGTH_SHORT).show();
                                if (userSelectedMarker != null) {
                                    userSelectedMarker.remove();
                                    userSelectedMarker = null;
                                }
                                if (actionButtonsLayout != null) {
                                    actionButtonsLayout.setVisibility(View.GONE);
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(AdminLocationActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

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
                        alertMessage = "⚠️ " + (location != null ? location : "Boya") + " ay may hindi awtorisadong galaw!";
                        break;
                    }
                }
                if (alertBannerLayout != null) {
                    alertBannerLayout.setVisibility(hasAlert ? View.VISIBLE : View.GONE);
                    if (hasAlert && tvAlertBanner != null) tvAlertBanner.setText(alertMessage);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadSavedSpots() {
        mDatabase.child("all_saved_spots").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                savedSpotList.clear();
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
                adminSavedSpotAdapter.notifyDataSetChanged();
                if (tvSpotsCount != null) {
                    tvSpotsCount.setText(savedSpotList.size() + " total shared spots");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void deleteSavedSpot(SavedSpot spot) {
        new AlertDialog.Builder(this)
                .setTitle("Burahin ang Spot")
                .setMessage("Sigurado ka bang gusto mong burahin ang \"" + spot.name + "\"?")
                .setPositiveButton("Oo", (d, w) -> {
                    mDatabase.child("all_saved_spots").child(spot.spotId).removeValue()
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, "Na-delete na.", Toast.LENGTH_SHORT).show();

                                if (savedSpotMarkers.containsKey(spot.spotId)) {
                                    Marker marker = savedSpotMarkers.get(spot.spotId);
                                    if (marker != null) {
                                        marker.remove();
                                    }
                                    savedSpotMarkers.remove(spot.spotId);
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hindi", null).show();
    }

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