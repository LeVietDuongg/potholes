package com.example.detectapplication2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Profile extends AppCompatActivity {
    private TextView tvEmail, tvUsername, tvPassword;
    private Button btnEdit, btnBack;
    private FirebaseAuth mAuth;
    private String uid;
    private boolean isPasswordVisible = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Ánh xạ các thành phần giao diện
        tvEmail = findViewById(R.id.tv_email);
        tvUsername = findViewById(R.id.tv_username);
        tvPassword = findViewById(R.id.tv_password);
        btnEdit = findViewById(R.id.btn_edt);
        btnBack = findViewById(R.id.btn_back);

        // Khởi tạo FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser().getUid(); // Lấy UID của người dùng hiện tại

        // Lấy dữ liệu từ Realtime Database
        getUserDataFromDatabase();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ProfileActivity", "Navigating to SettingFragment");

                // Chuyển về MainActivity2
                Intent intent = new Intent(Profile.this, MainActivity2.class);
                intent.putExtra("fragment", "setting"); // Gửi thông tin để chuyển tới SettingFragment
                startActivity(intent);
            }
        });

        // Sự kiện nút "Edit"
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ProfileActivity", "Navigating to EditProfile");
                Intent intent = new Intent(Profile.this, EditProfile.class);
                startActivity(intent);
            }
        });

        tvPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = tvPassword.getCompoundDrawables()[2]; // Lấy drawableEnd
                if (drawableEnd != null && event.getRawX() >= (tvPassword.getRight() - drawableEnd.getBounds().width())) {
                    togglePasswordVisibility(tvPassword);
                    return true;
                }
            }
            return false;
        });
    }

    private void getUserDataFromDatabase() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String emailFromDB = snapshot.child("email").getValue(String.class);
                    String usernameFromDB = snapshot.child("name").getValue(String.class);
                    String passwordFromDB = snapshot.child("password").getValue(String.class);

                    // Cập nhật TextView với dữ liệu từ Realtime Database
                    tvEmail.setText(emailFromDB);
                    tvUsername.setText(usernameFromDB);
                    tvPassword.setText(passwordFromDB);
                } else {
                    Toast.makeText(Profile.this, "Không tìm thấy thông tin người dùng.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Profile.this, "Lỗi khi kết nối: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void togglePasswordVisibility(TextView tvPassword) {
        if (isPasswordVisible) {
            // Chuyển về dạng ẩn mật khẩu
            tvPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            tvPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.password_icon, 0);
        } else {
            // Chuyển về dạng hiện mật khẩu
            tvPassword.setInputType(InputType.TYPE_CLASS_TEXT);
            tvPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.password_icon, 0);
        }
        isPasswordVisible = !isPasswordVisible;
    }

}