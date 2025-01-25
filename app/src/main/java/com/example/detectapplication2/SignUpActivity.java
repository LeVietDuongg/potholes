package com.example.detectapplication2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {
    private EditText edtUsername, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnSignUp, btnBack;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private boolean isPasswordVisible = false;

    private final String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Ánh xạ view
        edtUsername = findViewById(R.id.edt_username);
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        edtConfirmPassword = findViewById(R.id.edt_confirm_password);
        btnSignUp = findViewById(R.id.btn_signup);
        btnBack = findViewById(R.id.btn_back);

        // Firebase Authentication và Database Reference
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Xử lý sự kiện nút "Đăng ký"
        btnSignUp.setOnClickListener(v -> validateAndSignUp());

        // Xử lý sự kiện nút "Quay lại"
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        edtPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = edtPassword.getCompoundDrawables()[2]; // Lấy drawableEnd
                if (drawableEnd != null && event.getRawX() >= (edtPassword.getRight() - drawableEnd.getBounds().width())) {
                    togglePasswordVisibility(edtPassword);
                    return true;
                }
            }
            return false;
        });

        edtConfirmPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = edtConfirmPassword.getCompoundDrawables()[2]; // Lấy drawableEnd
                if (drawableEnd != null && event.getRawX() >= (edtConfirmPassword.getRight() - drawableEnd.getBounds().width())) {
                    togglePasswordVisibility(edtConfirmPassword);
                    return true;
                }
            }
            return false;
        });
    }

    // Hàm validate dữ liệu và tạo tài khoản
    private void validateAndSignUp() {
        String username = edtUsername.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        // Kiểm tra dữ liệu nhập vào
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
        } else if (!email.matches(emailPattern)) {
            edtEmail.setError("Email không đúng định dạng");
        } else if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
        } else if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu không khớp");
        } else {
            createAccount(username, email, password, confirmPassword);
        }
    }

    // Hàm tạo tài khoản trong Firebase Authentication và lưu vào Realtime Database
    private void createAccount(String username, String email, String password, String confirmPassword) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Lấy UID của người dùng vừa được tạo
                        String userId = mAuth.getCurrentUser().getUid();

                        // Lưu thông tin người dùng vào Realtime Database
                        Users user = new Users(username, email, password, confirmPassword);
                        databaseReference.child(userId).setValue(user)
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        sendVerificationEmail();
                                    } else {
                                        handleError("Lỗi lưu dữ liệu", task2.getException());
                                    }
                                });
                    } else {
                        handleError("Lỗi tạo tài khoản", task.getException());
                    }
                });
    }

    // Hàm gửi email xác minh
    private void sendVerificationEmail() {
        mAuth.getCurrentUser().sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email xác minh đã được gửi. Vui lòng kiểm tra hộp thư của bạn.", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        handleError("Lỗi gửi email xác minh", task.getException());
                    }
                });
    }

    // Hàm xử lý lỗi chung
    private void handleError(String message, Exception exception) {
        Log.e("SignUpError", message + ": " + exception.getMessage());
        Toast.makeText(this, message + ": " + exception.getMessage(), Toast.LENGTH_SHORT).show();
    }

    // Điều hướng về MainActivity
    private void navigateToMainActivity() {
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void togglePasswordVisibility(EditText edtPassword) {
        if (isPasswordVisible) {
            // Chuyển về dạng ẩn mật khẩu
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            edtPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.password_icon, 0);
        } else {
            // Chuyển về dạng hiện mật khẩu
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT);
            edtPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.password_icon, 0);
        }
        isPasswordVisible = !isPasswordVisible;

        // Đặt lại con trỏ ở cuối văn bản
        edtPassword.setSelection(edtPassword.getText().length());
    }

}