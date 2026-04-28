package com.example.fishertech;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etLocation, etBoatNumber;
    private Button btnSave, btnChangePhoto;
    private ImageView btnBack, ivProfilePic;

    private Uri imageUri;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivProfilePic.setImageURI(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        ivProfilePic.setOnClickListener(v -> openGallery());
        btnChangePhoto.setOnClickListener(v -> openGallery());

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            saveProfileData();
        });
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etLocation = findViewById(R.id.et_location);
        etBoatNumber = findViewById(R.id.et_boat_number);

        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        ivProfilePic = findViewById(R.id.iv_edit_profile_pic);

        btnChangePhoto = findViewById(R.id.btnChangePhoto);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void saveProfileData() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String boat = etBoatNumber.getText().toString().trim();

        if (name.isEmpty() || boat.isEmpty()) {
            Toast.makeText(this, "Pangalan at Numero ng Bangka ay kailangan.", Toast.LENGTH_SHORT).show();
            return;
        }


        Toast.makeText(this, "Matagumpay na na-update ang iyong profile!", Toast.LENGTH_SHORT).show();

        finish();
    }
}