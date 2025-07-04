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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Profile extends AppCompatActivity {
    private EditText tvEmail, tvUsername, tvPassword;
    private Button btnEdit, btnBack;
    private FirebaseAuth mAuth;
    private String uid;
    private boolean isPasswordVisible = false;
    private boolean isEditMode = false;

    // Biến để lưu thông tin ban đầu
    private String originalUsername = "";
    private String originalEmail = "";
    private String originalPassword = "";

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
        uid = mAuth.getCurrentUser().getUid();

        // Lấy dữ liệu từ Realtime Database
        getUserDataFromDatabase();

        // Thiết lập chế độ chỉ đọc ban đầu
        setEditMode(false);
        
        // Đảm bảo EditText có thể nhận focus khi cần
        tvUsername.setClickable(true);
        tvEmail.setClickable(true);
        tvPassword.setClickable(true);

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

        // Sự kiện nút "Edit Profile"
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEditMode) {
                    // Chuyển sang chế độ chỉnh sửa
                    setEditMode(true);
                    btnEdit.setText("SAVE");
                    Toast.makeText(Profile.this, "Bạn có thể chỉnh sửa thông tin", Toast.LENGTH_SHORT).show();
                    
                    // Debug: Kiểm tra trạng thái EditText
                    Log.d("Profile", "EditMode enabled - Username enabled: " + tvUsername.isEnabled() + 
                          ", Email enabled: " + tvEmail.isEnabled() + 
                          ", Password enabled: " + tvPassword.isEnabled());
                } else {
                    // Lưu thay đổi
                    saveChanges();
                }
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

    private void setEditMode(boolean editMode) {
        isEditMode = editMode;
        tvUsername.setEnabled(editMode);
        tvEmail.setEnabled(editMode);
        tvPassword.setEnabled(editMode);
        
        if (editMode) {
            tvUsername.setFocusableInTouchMode(true);
            tvEmail.setFocusableInTouchMode(true);
            tvPassword.setFocusableInTouchMode(true);
            tvUsername.setFocusable(true);
            tvEmail.setFocusable(true);
            tvPassword.setFocusable(true);
        } else {
            tvUsername.setFocusableInTouchMode(false);
            tvEmail.setFocusableInTouchMode(false);
            tvPassword.setFocusableInTouchMode(false);
            tvUsername.setFocusable(false);
            tvEmail.setFocusable(false);
            tvPassword.setFocusable(false);
        }
    }

    private void saveChanges() {
        String newUsername = tvUsername.getText().toString().trim();
        String newEmail = tvEmail.getText().toString().trim();
        String newPassword = tvPassword.getText().toString().trim();

        // Kiểm tra dữ liệu
        if (newUsername.isEmpty() || newEmail.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra email hợp lệ
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra xem có thay đổi gì không
        if (newUsername.equals(originalUsername) && 
            newEmail.equals(originalEmail) && 
            newPassword.equals(originalPassword)) {
            Toast.makeText(this, "Không có thay đổi nào", Toast.LENGTH_SHORT).show();
            setEditMode(false);
            btnEdit.setText("EDIT PROFILE");
            return;
        }

        // Hiển thị loading
        btnEdit.setEnabled(false);
        btnEdit.setText("SAVING...");

        // Cập nhật vào Firebase
        updateProfileInDatabase(newUsername, newEmail, newPassword);
    }

    private void updateProfileInDatabase(String newUsername, String newEmail, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            // Cập nhật vào Realtime Database
            if (!newUsername.equals(originalUsername)) {
                userRef.child("name").setValue(newUsername);
            }
            if (!newEmail.equals(originalEmail)) {
                userRef.child("email").setValue(newEmail);
            }
            if (!newPassword.equals(originalPassword)) {
                userRef.child("password").setValue(newPassword);
                userRef.child("confirmPassword").setValue(newPassword);
            }

            // Cập nhật Firebase Authentication nếu email thay đổi
            if (!newEmail.equals(originalEmail)) {
                user.updateEmail(newEmail)
                        .addOnSuccessListener(aVoid -> {
                            // Cập nhật password nếu cần
                            if (!newPassword.equals(originalPassword)) {
                                user.updatePassword(newPassword)
                                        .addOnSuccessListener(aVoid1 -> {
                                            onUpdateSuccess();
                                        })
                                        .addOnFailureListener(e -> {
                                            onUpdateFailure("Lỗi khi cập nhật mật khẩu: " + e.getMessage());
                                        });
                            } else {
                                onUpdateSuccess();
                            }
                        })
                        .addOnFailureListener(e -> {
                            onUpdateFailure("Lỗi khi cập nhật email: " + e.getMessage());
                        });
            } else {
                // Chỉ cập nhật password
                if (!newPassword.equals(originalPassword)) {
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid -> {
                                onUpdateSuccess();
                            })
                            .addOnFailureListener(e -> {
                                onUpdateFailure("Lỗi khi cập nhật mật khẩu: " + e.getMessage());
                            });
                } else {
                    onUpdateSuccess();
                }
            }
        }
    }

    private void onUpdateSuccess() {
        // Cập nhật thông tin ban đầu
        originalUsername = tvUsername.getText().toString().trim();
        originalEmail = tvEmail.getText().toString().trim();
        originalPassword = tvPassword.getText().toString().trim();

        // Chuyển về chế độ chỉ đọc
        setEditMode(false);
        btnEdit.setText("EDIT PROFILE");
        btnEdit.setEnabled(true);

        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

        // Quay về Setting
        Intent intent = new Intent(Profile.this, MainActivity2.class);
        intent.putExtra("fragment", "setting");
        startActivity(intent);
    }

    private void onUpdateFailure(String errorMessage) {
        btnEdit.setEnabled(true);
        btnEdit.setText("SAVE");
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void getUserDataFromDatabase() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    originalEmail = snapshot.child("email").getValue(String.class);
                    originalUsername = snapshot.child("name").getValue(String.class);
                    originalPassword = snapshot.child("password").getValue(String.class);

                    // Cập nhật EditText với dữ liệu từ Realtime Database
                    tvEmail.setText(originalEmail);
                    tvUsername.setText(originalUsername);
                    tvPassword.setText(originalPassword);
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

    private void togglePasswordVisibility(EditText tvPassword) {
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