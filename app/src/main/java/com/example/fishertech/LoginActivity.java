package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText email, password;
    private Button loginBtn;
    private TextView tvSignUp, tvForgotPassword;
    private ImageView togglePassword;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        togglePassword = findViewById(R.id.togglePassword);

        loginBtn.setOnClickListener(v -> loginUser());
        tvSignUp.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> resetPassword());

        togglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(android.R.drawable.ic_menu_view);
                isPasswordVisible = false;
            } else {
                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                isPasswordVisible = true;
            }
            password.setSelection(password.getText().length());
        });
    }

    private void loginUser() {
        String userEmail = email.getText().toString().trim();
        String userPass = password.getText().toString().trim();

        if (TextUtils.isEmpty(userEmail) || TextUtils.isEmpty(userPass)) {
            Toast.makeText(this, "Punan ang lahat ng detalye", Toast.LENGTH_SHORT).show();
            return;
        }

        loginBtn.setEnabled(false);
        mAuth.signInWithEmailAndPassword(userEmail, userPass)
                .addOnCompleteListener(task -> {
                    loginBtn.setEnabled(true);
                    if (task.isSuccessful()) {
                        checkUserRole();
                    } else {
                        Toast.makeText(this, "Mali ang account details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {

            mDatabase.child("FisherTech").child("Users").child(user.getUid())
                    .child("last_login").setValue(System.currentTimeMillis());
            // ADMIN QUICK CHECK
            if ("admin@fishertech.com".equals(user.getEmail())) {
                startActivity(new Intent(LoginActivity.this, DashboardAdminActivity.class));
                finish();
                return;
            }

            // CHECK IF USER RECORD EXISTS IN DATABASE
            mDatabase.child("FisherTech").child("Users").child(user.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // Check if disabled attribute exists
                                Boolean isDisabled = snapshot.child("disable").getValue(Boolean.class);
                                if (isDisabled != null && isDisabled) {
                                    mAuth.signOut();
                                    Toast.makeText(LoginActivity.this, "Your account is disabled.", Toast.LENGTH_LONG).show();
                                } else {
                                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                    finish();
                                }
                            } else {
                                // ACCOUNT DELETED BY ADMIN
                                mAuth.signOut();
                                Toast.makeText(LoginActivity.this, "Your account record was removed by Admin.", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void resetPassword() {
        String userEmail = email.getText().toString().trim();
        if (TextUtils.isEmpty(userEmail)) {
            email.setError("Ilagay ang email");
            return;
        }
        mAuth.sendPasswordResetEmail(userEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) Toast.makeText(this, "Reset link sent.", Toast.LENGTH_SHORT).show();
        });
    }
}