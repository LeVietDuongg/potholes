package com.example.detectapplication2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {
    private FirebaseAuth mAuth;
    private TextView UserName, Total;
    private String uid;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        UserName = view.findViewById(R.id.username);
        Total = view.findViewById(R.id.total_pothole);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String usernameFromDB = snapshot.child("name").getValue(String.class);

                        // Cập nhật TextView với dữ liệu từ Realtime Database
                        UserName.setText(usernameFromDB);
                    } else {
                        Toast.makeText(getActivity(), "Không tìm thấy thông tin người dùng.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });
        }

        fetchPotholesData();

        return view;
    }

    private void fetchPotholesData() {
        DatabaseReference potholesRef = FirebaseDatabase.getInstance().getReference("potholes");

        potholesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalPotholes = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    totalPotholes++;
                }

                // Hiển thị tổng số pothole
                if (Total != null) {
                    Total.setText(String.valueOf(totalPotholes));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Error fetching potholes data.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}