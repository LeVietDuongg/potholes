package com.example.detectapplication2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PothethonListActivity extends AppCompatActivity {
    private PieChart mypiechart;
    private ArrayList<PieEntry> piedata;
    private FirebaseAuth mAuth;
    private TextView UserName, potholeListTextView;
    private String uid;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude;
    private double currentLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pothethonlist);

        UserName = findViewById(R.id.user_name);
        potholeListTextView = findViewById(R.id.potholeListTextView);
        mypiechart = findViewById(R.id.pie_chart);
        piedata = new ArrayList<>();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Yêu cầu quyền truy cập vị trí nếu chưa được cấp
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        // Lấy tọa độ hiện tại
        getCurrentLocation();

        // Hiển thị PieChart ban đầu (rỗng)
        updatePieChart(0, 0, 0);

        // Lấy dữ liệu người dùng
        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String usernameFromDB = snapshot.child("name").getValue(String.class);
                    UserName.setText(usernameFromDB);
                } else {
                    Toast.makeText(PothethonListActivity.this, "Không tìm thấy thông tin người dùng.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();

                    // Sau khi lấy tọa độ hiện tại, fetch dữ liệu potholes
                    fetchPotholeData();
                    fetchNearbyPotholes();
                } else {
                    Toast.makeText(PothethonListActivity.this, "Không thể lấy vị trí hiện tại.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchPotholeData() {
        DatabaseReference potholeRef = FirebaseDatabase.getInstance().getReference("potholes");
        potholeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int heavyCount = 0;
                int mediumCount = 0;
                int lightCount = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    String level = data.child("level").getValue(String.class);
                    if (level != null) {
                        switch (level) {
                            case "heavy":
                                heavyCount++;
                                break;
                            case "medium":
                                mediumCount++;
                                break;
                            case "light":
                                lightCount++;
                                break;
                        }
                    }
                }

                updatePieChart(heavyCount, mediumCount, lightCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PothethonListActivity.this, "Error fetching pothole data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchNearbyPotholes() {
        DatabaseReference potholesRef = FirebaseDatabase.getInstance().getReference("potholes");
        potholesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> potholeDetails = new ArrayList<>();
                int stt = 1;

                for (DataSnapshot data : snapshot.getChildren()) {
                    String address = data.child("address").getValue(String.class);
                    Double latitude = data.child("latitude").getValue(Double.class);
                    Double longitude = data.child("longitude").getValue(Double.class);

                    if (address != null && latitude != null && longitude != null) {
                        double distance = calculateDistance(currentLatitude, currentLongitude, latitude, longitude);

                        if (distance <= 1000) { // Filter potholes within a 1 km radius
                            potholeDetails.add(stt++ + ". " + address + " - " + String.format("%.2f", distance / 1000) + " km");
                        }
                    }
                }

                if (!potholeDetails.isEmpty()) {
                    potholeListTextView.setText(String.join("\n", potholeDetails));
                } else {
                    potholeListTextView.setText("Không có pothole nào gần bạn.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PothethonListActivity.this, "Error fetching pothole data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double calculateDistance(double startLat, double startLng, double endLat, double endLng) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(startLat, startLng, endLat, endLng, results);
        return results[0];
    }

    private void updatePieChart(int heavy, int medium, int light) {
        piedata.clear();
        piedata.add(new PieEntry(heavy, "Heavy"));
        piedata.add(new PieEntry(medium, "Medium"));
        piedata.add(new PieEntry(light, "Light"));

        PieDataSet mypieDataSet = new PieDataSet(piedata, "Pothole Severity Levels");
        mypieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        mypieDataSet.setValueTextColor(Color.BLACK);
        mypieDataSet.setValueTextSize(16f);

        PieData mypieData = new PieData(mypieDataSet);

        mypiechart.setData(mypieData);
        mypiechart.getDescription().setEnabled(false);
        mypiechart.setCenterText("Pothole Severity");
        mypiechart.animate();
    }
}