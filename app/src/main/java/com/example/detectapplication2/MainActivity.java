package com.example.detectapplication2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    Button btnSignIn, btnSignUp;
    TextView txtForgotPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btnSignIn = findViewById(R.id.btn_signin);
        btnSignUp = findViewById(R.id.btn_signup);
        txtForgotPassword = findViewById(R.id.txt_quenmk);

        btnSignIn.setOnClickListener(v -> {
            // Xử lý khi nút "Sign In" được nhấn
            Intent myintent = new Intent(this, SignInActivity.class);
            startActivity(myintent);
        });
        btnSignUp.setOnClickListener(v -> {
            // Xử lý khi nút "Sign Up" được nhấn
            Intent myintent = new Intent(this, SignUpActivity.class);
            startActivity(myintent);
        });

        txtForgotPassword.setOnClickListener(v -> {
            // Xử lý khi nút "Forgot Password" được nhấn
            Intent myintent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(myintent);
        });
    }
}