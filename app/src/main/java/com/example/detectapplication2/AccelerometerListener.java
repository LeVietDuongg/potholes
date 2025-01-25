package com.example.detectapplication2;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class AccelerometerListener implements SensorEventListener {
    private Context context;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private static final double THRESHOLD_LIGHT = 30.0;
    private static final double THRESHOLD_MEDIUM = 40.0;
    private static final double THRESHOLD_HEAVY = 50.0;
    private static final double MOVEMENT_THRESHOLD = 20.0; // Ngưỡng chuyển động bắt đầu

    private boolean dialogShown = false; // Kiểm soát việc hiển thị dialog
    private boolean isMoving = false; // Kiểm tra xem điện thoại có đang di chuyển không
    private long movementStartTime = 0; // Thời gian bắt đầu chuyển động

    // Queue để lưu trữ các giá trị magnitude và thời gian của chúng
    private Queue<Long> timestamps = new LinkedList<>();
    private Queue<Double> magnitudes = new LinkedList<>();

    // Thời gian xác định 2 giây
    private static final long BUFFER_TIME_MS = 2000; // 2 giây

    public AccelerometerListener(Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        // Cấu hình LocationRequest
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000) // 10 giây
                .setFastestInterval(5000); // 5 giây

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Log.d("FusedLocation", "Location updated: " + locationResult.getLastLocation());
            }
        };

        // Khởi tạo cảm biến gia tốc
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Đăng ký lắng nghe cảm biến
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            double magnitude = Math.sqrt(x * x + y * y + z * z);
            long currentTime = System.currentTimeMillis();

            Log.d("AccelerometerListener", "Magnitude: " + magnitude);

            // Kiểm tra xem điện thoại có đang di chuyển không
            if (!isMoving && magnitude > MOVEMENT_THRESHOLD) {
                isMoving = true;
                movementStartTime = currentTime;
                Log.d("AccelerometerListener", "Phone started moving at: " + movementStartTime);
            }

            // Nếu điện thoại đang di chuyển và thời gian đã trôi qua 2 giây, bắt đầu lưu trữ magnitude
            if (isMoving) {
                // Thêm giá trị magnitude vào buffer cùng với thời gian
                magnitudes.add(magnitude);
                timestamps.add(currentTime);

                Log.d("AccelerometerListener", "Buffer Size: " + magnitudes.size());

                // Xóa các giá trị trong buffer nếu thời gian quá 2 giây
                while (!timestamps.isEmpty() && currentTime - timestamps.peek() > BUFFER_TIME_MS) {
                    magnitudes.poll();
                    timestamps.poll();
                }

                // Kiểm tra nếu đã đủ 2 giây
                if (currentTime - movementStartTime >= BUFFER_TIME_MS) {
                    double maxMagnitude = getMaxMagnitude();
                    Log.d("AccelerometerListener", "Max Magnitude after 2 seconds: " + maxMagnitude);

                    if (maxMagnitude > THRESHOLD_LIGHT && !dialogShown) {
                        String level = determineLevel(maxMagnitude);
                        Toast.makeText(context, "Pothole detected!", Toast.LENGTH_SHORT).show();
                        requestLocationAndShowDialog(level);
                    }
                    isMoving = false;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không cần xử lý trong trường hợp này
    }

    private String determineLevel(double maxMagnitude) {
        if (maxMagnitude < THRESHOLD_MEDIUM) {
            return "light";
        } else if (maxMagnitude < THRESHOLD_HEAVY) {
            return "medium";
        } else {
            return "heavy";
        }
    }

    private void requestLocationAndShowDialog(String level) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permission is not granted!", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                showConfirmationDialog(location.getLatitude(), location.getLongitude(), level);
            } else {
                Log.e("FusedLocation", "Last location is null");
                Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfirmationDialog(double latitude, double longitude, String level) {
        dialogShown = true;
        String address = String.format(Locale.getDefault(), "Lat: %.5f, Lon: %.5f", latitude, longitude);

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Pothole Detected");
                builder.setMessage("Confirm pothole at:\n" + address + "\nSeverity: " + level);
                builder.setPositiveButton("Confirm", (dialog, which) -> {
                    savePotholeToDatabase(latitude, longitude, address, level);
                    dialogShown = false;
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialogShown = false);
                builder.show();
            });
        }
    }

    private double getMaxMagnitude() {
        double max = 0;
        for (double magnitude : magnitudes) {
            if (magnitude > max) {
                max = magnitude;
            }
        }
        return max;
    }

    private void savePotholeToDatabase(double latitude, double longitude, String address, String level) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("potholes");

        String userId = auth.getUid();
        if (userId != null) {
            Pothole pothole = new Pothole(latitude, longitude, address, userId, level);
            database.push().setValue(pothole);
        }
    }

    public void unregisterListeners() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
}
