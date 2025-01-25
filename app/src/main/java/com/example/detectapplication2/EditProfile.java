package com.example.detectapplication2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditProfile extends AppCompatActivity {

    Button btnSave, btnBack;
    EditText edtEmail, edtUsername, edtPassword, edtCPassword;
    private FirebaseAuth mAuth;
    private String uid;
    private boolean isPasswordVisible = false;

    // Biến để lưu thông tin ban đầu từ Realtime Database
    private String originalUsername = "";
    private String originalEmail = "";
    private String originalPassword = "";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edt_profile);

        btnSave = findViewById(R.id.btn_save);
        btnBack = findViewById(R.id.btn_back);

        edtUsername = findViewById(R.id.edt_username);
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        edtCPassword = findViewById(R.id.edt_confirm_password);

        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser().getUid();

        // Lấy dữ liệu từ Realtime Database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    originalUsername = snapshot.child("name").getValue(String.class);
                    originalEmail = snapshot.child("email").getValue(String.class);
                    originalPassword = snapshot.child("password").getValue(String.class);

                    // Hiển thị dữ liệu ban đầu vào EditText
                    edtUsername.setText(originalUsername);
                    edtEmail.setText(originalEmail);
                    edtPassword.setText(originalPassword);
                } else {
                    Toast.makeText(EditProfile.this, "Không tìm thấy thông tin người dùng.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfile.this, "Lỗi khi lấy dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Sự kiện nút "Back"
        btnBack.setOnClickListener(v -> finish());

        // Sự kiện nút "Save"
        btnSave.setOnClickListener(v -> {
            String newUsername = edtUsername.getText().toString().trim();
            String newEmail = edtEmail.getText().toString().trim();
            String newPassword = edtPassword.getText().toString().trim();
            String confirmPassword = edtCPassword.getText().toString().trim();

            // Kiểm tra thay đổi và cập nhật
            if (isProfileChanged(newUsername, newEmail, newPassword)) {
                updateProfileInDatabase(newUsername, newEmail, newPassword, confirmPassword);
            } else {
                Toast.makeText(EditProfile.this, "Không có thay đổi nào.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(EditProfile.this, Profile.class);
                startActivity(intent);
                finish();
            }
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

        edtCPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = edtCPassword.getCompoundDrawables()[2]; // Lấy drawableEnd
                if (drawableEnd != null && event.getRawX() >= (edtCPassword.getRight() - drawableEnd.getBounds().width())) {
                    togglePasswordVisibility(edtCPassword);
                    return true;
                }
            }
            return false;
        });
    }

    // Kiểm tra xem thông tin có thay đổi so với dữ liệu ban đầu không
    private boolean isProfileChanged(String newUsername, String newEmail, String newPassword) {
        return !newUsername.equals(originalUsername) ||
                !newEmail.equals(originalEmail) ||
                !newPassword.equals(originalPassword);
    }

    // Cập nhật thông tin vào Realtime Database và Authentication
    private void updateProfileInDatabase(String newUsername, String newEmail, String newPassword, String confirmPassword) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp.", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

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

            if (!newEmail.equals(originalEmail)) {
                user.updateEmail(newEmail)
                        .addOnSuccessListener(aVoid -> {
                            if (!newPassword.equals(originalPassword)) {
                                user.updatePassword(newPassword)
                                        .addOnSuccessListener(aVoid1 -> {
                                            Toast.makeText(EditProfile.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(EditProfile.this, "Lỗi khi cập nhật mật khẩu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(EditProfile.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(EditProfile.this, "Lỗi khi cập nhật email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                if (!newPassword.equals(originalPassword)) {
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(EditProfile.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(EditProfile.this, Profile.class);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(EditProfile.this, "Lỗi khi cập nhật mật khẩu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(EditProfile.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
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
