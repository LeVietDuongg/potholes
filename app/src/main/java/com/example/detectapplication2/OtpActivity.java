package com.example.detectapplication2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class OtpActivity extends AppCompatActivity {
    Button btn_done_otp;
    EditText edt_otp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp);

        btn_done_otp = findViewById(R.id.btn_done_otp);
        edt_otp = findViewById(R.id.edt_otp);

        btn_done_otp.setOnClickListener(v -> {
            // xu ly backend ở đây



            finish();
        });

    }
}