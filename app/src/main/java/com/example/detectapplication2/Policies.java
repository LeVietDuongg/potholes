package com.example.detectapplication2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Policies extends AppCompatActivity {
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policies);

        btnBack = findViewById(R.id.btnBack); // Initialize btnBack

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("PoliciesActivity", "Navigating to SettingFragment");

                // Chuyển về MainActivity2
                Intent intent = new Intent(Policies.this, MainActivity2.class);
                intent.putExtra("fragment", "setting"); // Gửi thông tin để chuyển tới SettingFragment
                startActivity(intent);
            }
        });
    }
}