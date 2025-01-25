package com.example.detectapplication2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class SignInActivity extends AppCompatActivity {

    private Button btnBack, btnLogin;
    private EditText edtEmail, edtPassword;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;
    private final String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    private CheckBox chkRememberMe;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Ánh xạ các thành phần giao diện
        initViews();

        // Khởi tạo FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Thiết lập sự kiện cho các nút
        initListeners();

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

        if (sharedPreferences.getBoolean("remember", false)) {
            String savedUid = sharedPreferences.getString("uid", "");
            String savedName = sharedPreferences.getString("name", "");
            String savedEmail = sharedPreferences.getString("email", "");
            String savedPassword = sharedPreferences.getString("password", "");

            edtEmail.setText(savedEmail);
            edtPassword.setText(savedPassword);
            chkRememberMe.setChecked(true);

            // Tự động chuyển đến màn hình chính
            Intent intent = new Intent(SignInActivity.this, MainActivity2.class);
            intent.putExtra("uid", savedUid);
            intent.putExtra("name", savedName);
            intent.putExtra("email", savedEmail);
            startActivity(intent);
            finish();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnLogin = findViewById(R.id.btn_login);
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);

        chkRememberMe = findViewById(R.id.ckb_remember);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void initListeners() {
        // Sự kiện nút "Back"
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Sự kiện nút "Login"
        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            // Kiểm tra dữ liệu đầu vào
            if (validateEmail(email) && validatePassword(password)) {
                loginUser(email,password);
            }
        });
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            edtEmail.setError("Email không được để trống");
            edtEmail.requestFocus();
            return false;
        } else if (!email.matches(emailPattern)) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return false;
        } else {
            edtEmail.setError(null);
            return true;
        }
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            edtPassword.setError("Password không được để trống");
            edtPassword.requestFocus();
            return false;
        } else if (password.length() < 6) {
            edtPassword.setError("Password phải có ít nhất 6 ký tự");
            edtPassword.requestFocus();
            return false;
        } else {
            edtPassword.setError(null);
            return true;
        }
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            String uid = user.getUid();
                            updatePasswordInDatabase(uid, password);
                            Log.d("UID Authentication", "Logged-in User: " + uid);
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
                            reference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        String name = snapshot.child("name").getValue(String.class);
                                        String emailFromDB = snapshot.child("email").getValue(String.class);

                                        Intent intent = new Intent(SignInActivity.this, SplashActivity.class);
                                        if (chkRememberMe.isChecked()) {
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putBoolean("remember", true);
                                            editor.putString("uid", uid);
                                            editor.putString("name", name);
                                            editor.putString("email", emailFromDB);
                                            editor.putString("password", password);
                                            editor.apply();
                                        } else {
                                            clearSharedPreferences();
                                        }
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(SignInActivity.this, "Không thể lấy thông tin người dùng.", Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(SignInActivity.this, "Lỗi khi kết nối: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(this, "Vui lòng xác thực email trước khi đăng nhập", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("LoginError", "Error: ", task.getException());
                    }
                });
    }

    private void updatePasswordInDatabase(String uid, String newPassword) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        // Lấy mật khẩu hiện tại từ Realtime Database
        userRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentPassword = snapshot.getValue(String.class);

                    // So sánh mật khẩu
                    if (currentPassword != null && currentPassword.equals(newPassword)) {

                    } else {
                        // Nếu khác nhau, tiến hành cập nhật
                        userRef.child("password").setValue(newPassword)
                                .addOnSuccessListener(unused -> {
                                    // Cập nhật thêm trường confirmPassword
                                    userRef.child("confirmPassword").setValue(newPassword)
                                            .addOnSuccessListener(unused1 -> Toast.makeText(
                                                    SignInActivity.this,
                                                    "Cập nhật mật khẩu và xác nhận thành công",
                                                    Toast.LENGTH_SHORT
                                            ).show())
                                            .addOnFailureListener(e -> Toast.makeText(
                                                    SignInActivity.this,
                                                    "Cập nhật mật khẩu thành công nhưng lỗi khi cập nhật confirmPassword: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT
                                            ).show());
                                })
                                .addOnFailureListener(e -> Toast.makeText(
                                        SignInActivity.this,
                                        "Không thể cập nhật mật khẩu: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show());
                    }
                } else {
                    Toast.makeText(SignInActivity.this, "Không tìm thấy người dùng trong cơ sở dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SignInActivity.this, "Lỗi khi truy xuất cơ sở dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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

    private void clearSharedPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

}