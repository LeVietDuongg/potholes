package com.example.detectapplication2;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.detectapplication2.databinding.ActivityMain2Binding;

public class MainActivity2 extends AppCompatActivity {

    private ActivityMain2Binding binding;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private AccelerometerListener accelerometerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup fragments for navigation
        setupFragments();

        // Setup sensor manager and accelerometer listener
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometerListener = new AccelerometerListener(this);

        // Handle intent to display specific fragment
        handleFragmentIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register accelerometer listener
        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister accelerometer listener
        sensorManager.unregisterListener(accelerometerListener);
    }

    private void setupFragments() {
        // Set default fragment (HomeFragment)
        replaceFragment(new HomeFragment());

        // Handle navigation changes in BottomNavigationView
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.map) {
                replaceFragment(new MapFragment());
            } else if (item.getItemId() == R.id.setting) {
                replaceFragment(new SettingFragment());
            }
            return true;
        });
    }

    private void handleFragmentIntent() {
        Intent intent = getIntent();
        String fragment = intent.getStringExtra("fragment");

        if (fragment != null && fragment.equals("setting")) {
            replaceFragment(new SettingFragment());
            binding.bottomNavigationView.setSelectedItemId(R.id.setting);
        } else {
            replaceFragment(new HomeFragment());  // Default fragment is HomeFragment
            binding.bottomNavigationView.setSelectedItemId(R.id.home);
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment); // Replace with new fragment
        fragmentTransaction.commit();
    }
}
