package com.example.detectapplication2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText edtEmail;
    private Button btnContinue, btnBack;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Khởi tạo các thành phần
        edtEmail = findViewById(R.id.edt_email);
        btnContinue = findViewById(R.id.btn_continue);
        btnBack = findViewById(R.id.btn_back);
        mAuth = FirebaseAuth.getInstance();

        // Gửi yêu cầu đặt lại mật khẩu
        btnContinue.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                edtEmail.setError("Vui lòng nhập email");
                return;
            }
            sendResetPasswordEmail(email);
        });

        // Quay lại màn hình trước
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void sendResetPasswordEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            ForgotPasswordActivity.this,
                            "Liên kết đặt lại mật khẩu đã được gửi đến email của bạn",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Chuyển về MainActivity sau khi gửi thành công
                    Intent intent = new Intent(ForgotPasswordActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        ForgotPasswordActivity.this,
                        "Lỗi khi gửi email: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }

}
