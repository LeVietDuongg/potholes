package com.example.detectapplication2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Security extends AppCompatActivity {
    private TextView security, changepassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_security);
        security = findViewById(R.id.tv_edit_profile_insecurity);
        changepassword = findViewById(R.id.tv_change_password_in_security);

        changepassword.setOnClickListener(v -> {
            Intent intent = new Intent(Security.this, EditProfile.class);
            startActivity(intent);
            finish();
        });

        security.setOnClickListener(v -> {
            Intent intent = new Intent(Security.this, Profile.class);
            startActivity(intent);
            finish();
        });
    }
}