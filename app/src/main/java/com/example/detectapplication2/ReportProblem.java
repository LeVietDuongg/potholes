package com.example.detectapplication2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ReportProblem extends AppCompatActivity {

    Button btnback;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report_problem);
        btnback  = (Button) findViewById(R.id.btnBack);
        btnback.setOnClickListener(v -> {
            Toast.makeText(this, "Đã gửi, cảm ơn bạn! ", Toast.LENGTH_SHORT).show();

            finish();
        });
    }
}