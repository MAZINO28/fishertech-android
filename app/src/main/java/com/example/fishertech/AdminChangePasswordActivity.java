package com.example.fishertech;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class AdminChangePasswordActivity
        extends AppCompatActivity {

    private EditText etOldPassword,
            etNewPassword,
            etConfirmNewPassword;

    private Button btnUpdatePassword;

    private TextView tvCancel;

    private ImageView toggleOldPassword,
            toggleNewPassword,
            toggleConfirmPassword;

    private boolean oldVisible = false;
    private boolean newVisible = false;
    private boolean confirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_admin_change_password
        );

        etOldPassword =
                findViewById(R.id.etOldPassword);

        etNewPassword =
                findViewById(R.id.etNewPassword);

        etConfirmNewPassword =
                findViewById(R.id.etConfirmNewPassword);

        btnUpdatePassword =
                findViewById(R.id.btnUpdatePassword);

        tvCancel =
                findViewById(R.id.tvCancel);

        toggleOldPassword =
                findViewById(R.id.toggleOldPassword);

        toggleNewPassword =
                findViewById(R.id.toggleNewPassword);

        toggleConfirmPassword =
                findViewById(R.id.toggleConfirmPassword);

        // SHOW/HIDE OLD PASSWORD
        toggleOldPassword.setOnClickListener(v -> {

            if (oldVisible) {

                etOldPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_PASSWORD
                );

                oldVisible = false;

            } else {

                etOldPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );

                oldVisible = true;
            }

            etOldPassword.setSelection(
                    etOldPassword.getText().length()
            );
        });

        // SHOW/HIDE NEW PASSWORD
        toggleNewPassword.setOnClickListener(v -> {

            if (newVisible) {

                etNewPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_PASSWORD
                );

                newVisible = false;

            } else {

                etNewPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );

                newVisible = true;
            }

            etNewPassword.setSelection(
                    etNewPassword.getText().length()
            );
        });

        // SHOW/HIDE CONFIRM PASSWORD
        toggleConfirmPassword.setOnClickListener(v -> {

            if (confirmVisible) {

                etConfirmNewPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_PASSWORD
                );

                confirmVisible = false;

            } else {

                etConfirmNewPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );

                confirmVisible = true;
            }

            etConfirmNewPassword.setSelection(
                    etConfirmNewPassword.getText().length()
            );
        });

        // UPDATE PASSWORD
        btnUpdatePassword.setOnClickListener(
                v -> updatePassword()
        );

        // CANCEL
        tvCancel.setOnClickListener(v -> finish());
    }

    private void updatePassword() {

        String oldPass =
                etOldPassword.getText()
                        .toString()
                        .trim();

        String newPass =
                etNewPassword.getText()
                        .toString()
                        .trim();

        String confirmPass =
                etConfirmNewPassword.getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(oldPass) ||
                TextUtils.isEmpty(newPass) ||
                TextUtils.isEmpty(confirmPass)) {

            Toast.makeText(
                    this,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!newPass.equals(confirmPass)) {

            etConfirmNewPassword.setError(
                    "Passwords do not match"
            );

            return;
        }

        if (newPass.length() < 6) {

            etNewPassword.setError(
                    "Password must be at least 6 characters"
            );

            return;
        }

        Toast.makeText(
                this,
                "Admin password updated successfully!",
                Toast.LENGTH_LONG
        ).show();

        finish();
    }
}