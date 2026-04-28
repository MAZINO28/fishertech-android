package com.example.fishertech;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etOldPassword, etNewPassword, etConfirmNewPassword;
    private Button btnUpdatePassword;
    private TextView tvCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        tvCancel = findViewById(R.id.tvCancel);

        btnUpdatePassword.setOnClickListener(v -> updatePassword());

        tvCancel.setOnClickListener(v -> finish());
    }

    private void updatePassword() {
        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(this, "Mangyaring punan ang lahat ng fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmNewPassword.setError("Hindi magkatugma ang bagong password");
            return;
        }

        if (newPass.length() < 6) {
            etNewPassword.setError("Dapat hindi bababa sa 6 characters");
            return;
        }

        Toast.makeText(this, "Tagumpay ang pagpalit ng password!", Toast.LENGTH_LONG).show();
        finish();
    }
}