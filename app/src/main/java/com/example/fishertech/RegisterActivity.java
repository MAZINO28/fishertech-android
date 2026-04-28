package com.example.fishertech;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText regName, regEmail, regPhone, regPassword, regConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        regName = findViewById(R.id.regName);
        regEmail = findViewById(R.id.regEmail);
        regPhone = findViewById(R.id.regPhone);
        regPassword = findViewById(R.id.regPassword);
        regConfirmPassword = findViewById(R.id.regConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnRegister.setOnClickListener(v -> registerUser());

        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = regName.getText().toString().trim();
        String email = regEmail.getText().toString().trim();
        String phone = regPhone.getText().toString().trim();
        String pass = regPassword.getText().toString().trim();
        String confirmPass = regConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(phone) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Mangyaring punan ang lahat ng detalye", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 6) {
            regPassword.setError("Dapat hindi bababa sa 6 characters");
            regPassword.requestFocus();
            return;
        }

        if (!pass.equals(confirmPass)) {
            regConfirmPassword.setError("Hindi magkatugma ang password");
            regConfirmPassword.requestFocus();
            return;
        }

        Toast.makeText(this, "Tagumpay ang pag-register!", Toast.LENGTH_LONG).show();
        finish();
    }
}