package com.example.fishertech;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText regName,
            regEmail,
            regPhone,
            regLocation,
            regBoatNum,
            regPassword,
            regConfirmPassword;

    private Button btnRegister;

    private TextView tvLoginLink;

    private FirebaseAuth mAuth;

    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        // Firebase
        mAuth = FirebaseAuth.getInstance();

        dbRef = FirebaseDatabase
                .getInstance()
                .getReference();

        // Views
        regName = findViewById(R.id.regName);

        regEmail = findViewById(R.id.regEmail);

        regPhone = findViewById(R.id.regPhone);

        regLocation = findViewById(R.id.regLocation);

        regBoatNum = findViewById(R.id.regBoatNum);

        regPassword = findViewById(R.id.regPassword);

        regConfirmPassword =
                findViewById(R.id.regConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);

        tvLoginLink = findViewById(R.id.tvLoginLink);

        // Password Toggle
        setupPasswordToggle(regPassword);

        setupPasswordToggle(regConfirmPassword);

        // Register Button
        btnRegister.setOnClickListener(v -> registerUser());

        // Back to Login
        tvLoginLink.setOnClickListener(v -> finish());
    }

    // ================= PASSWORD TOGGLE =================

    private void setupPasswordToggle(EditText editText) {

        editText.setOnTouchListener((v, event) -> {

            final int DRAWABLE_RIGHT = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {

                if (event.getRawX() >=
                        (editText.getRight()
                                - editText.getCompoundDrawables()[DRAWABLE_RIGHT]
                                .getBounds().width()
                                - editText.getPaddingEnd())) {

                    // Show / Hide Password
                    if (editText.getTransformationMethod()
                            instanceof PasswordTransformationMethod) {

                        editText.setTransformationMethod(
                                HideReturnsTransformationMethod
                                        .getInstance()
                        );

                    } else {

                        editText.setTransformationMethod(
                                PasswordTransformationMethod
                                        .getInstance()
                        );
                    }

                    // Keep cursor at end
                    editText.setSelection(
                            editText.getText().length()
                    );

                    return true;
                }
            }

            return false;
        });
    }

    // ================= REGISTER FUNCTION =================

    private void registerUser() {

        String name =
                regName.getText().toString().trim();

        String email =
                regEmail.getText().toString().trim();

        String phone =
                regPhone.getText().toString().trim();

        String location =
                regLocation.getText().toString().trim();

        String boatNum =
                regBoatNum.getText().toString().trim();

        String pass =
                regPassword.getText().toString().trim();

        String confirmPass =
                regConfirmPassword.getText()
                        .toString()
                        .trim();

        // ================= VALIDATION =================

        if (TextUtils.isEmpty(name) ||
                TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(phone) ||
                TextUtils.isEmpty(location) ||
                TextUtils.isEmpty(boatNum) ||
                TextUtils.isEmpty(pass) ||
                TextUtils.isEmpty(confirmPass)) {

            Toast.makeText(
                    this,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (pass.length() < 6) {

            regPassword.setError(
                    "Password must be at least 6 characters"
            );

            regPassword.requestFocus();

            return;
        }

        if (!pass.equals(confirmPass)) {

            regConfirmPassword.setError(
                    "Passwords do not match"
            );

            regConfirmPassword.requestFocus();

            return;
        }

        // Disable button
        btnRegister.setEnabled(false);

        // ================= FIREBASE AUTH =================

        mAuth.createUserWithEmailAndPassword(
                        email,
                        pass
                )

                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        // Get UID
                        String userId =
                                mAuth.getCurrentUser()
                                        .getUid();

                        // Create user data
                        Map<String, Object> userData =
                                new HashMap<>();

                        userData.put("name", name);

                        userData.put("email", email);

                        userData.put("phone", phone);

                        userData.put(
                                "location",
                                location
                        );

                        userData.put(
                                "boat_number",
                                boatNum
                        );

                        userData.put(
                                "role",
                                "user"
                        );
                        userData.put("disable", false);

                        userData.put(
                                "created_at",
                                System.currentTimeMillis()
                        );

                        // ================= SAVE TO DATABASE =================

                        dbRef.child("FisherTech")
                                .child("Users")
                                .child(userId)
                                .setValue(userData)

                                .addOnCompleteListener(dbTask -> {

                                    btnRegister.setEnabled(true);

                                    if (dbTask.isSuccessful()) {

                                        Toast.makeText(
                                                RegisterActivity.this,
                                                "Registration Successful!",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        finish();

                                    } else {

                                        Toast.makeText(
                                                RegisterActivity.this,
                                                "Database Error: "
                                                        + dbTask.getException()
                                                        .getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                });

                    } else {

                        btnRegister.setEnabled(true);

                        Toast.makeText(
                                RegisterActivity.this,
                                "Authentication Failed: "
                                        + task.getException()
                                        .getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}