package com.example.detectapplication2;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {
    private FirebaseAuth mAuth;
    private TextView UserName, Total, multilinePotholes;
    private ImageView image1, image2, imageViewchart;
    private TextView temperatureText, humidityText, conditionText;
    private EditText cityInput;
    private Button searchButton;
    private String uid;
    private final String API_KEY = "90ec80953b809a18c31b89f696cf4b76";

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

        multilinePotholes = view.findViewById(R.id.multiLineTextView);

        // Initialize views
        image1 = view.findViewById(R.id.pothehole);

        imageViewchart = view.findViewById(R.id.imageViewchart);

        temperatureText = view.findViewById(R.id.temperature_text);
        humidityText = view.findViewById(R.id.humidity_text);
        conditionText = view.findViewById(R.id.condition_text);
        cityInput = view.findViewById(R.id.city_input);
        searchButton = view.findViewById(R.id.search_button);

        // Set click listeners for images
        image1.setOnClickListener(v -> startActivity(new Intent(getActivity(), PothethonListActivity.class)));
        imageViewchart.setOnClickListener(v -> startActivity(new Intent(getActivity(), PothethonListActivity.class)));

        mAuth = FirebaseAuth.getInstance();
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

            }
        });

        // Set up search button click listener
        searchButton.setOnClickListener(v -> {
            String city = cityInput.getText().toString().trim();
            if (!city.isEmpty()) {
                new GetWeatherTask(city).execute();
            }
        });

        // Fetch weather for Saigon on startup
        new GetWeatherTask("Binh Duong").execute();

        fetchPotholesData(multilinePotholes);

        return view;
    }

    private class GetWeatherTask extends AsyncTask<Void, Void, String> {
        private String city;

        public GetWeatherTask(String city) {
            this.city = city;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String response = "";
            try {
                URL url = new URL("https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY + "&units=metric");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response += line;
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONObject main = jsonObject.getJSONObject("main");
                double temp = main.getDouble("temp");
                int humidity = main.getInt("humidity");

                String weatherCondition = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");

                temperatureText.setText("Temperature: " + temp + " °C");
                humidityText.setText("Humidity: " + humidity + "%");
                conditionText.setText("Condition: " + weatherCondition);

                // kiểm tra thời tiết xấu thì hiển thị thông báo ra đường làm gì
                if (weatherCondition.contains("rain") || weatherCondition.contains("storm") || weatherCondition.contains("snow")) {
                    Toast.makeText(getActivity(), "Thời tiết đang xấu bạn nên hạn chế ra ngoài", Toast.LENGTH_LONG).show();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fetchPotholesData(TextView multilinePotholes) {
        DatabaseReference potholesRef = FirebaseDatabase.getInstance().getReference("potholes");

        potholesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalPotholes = 0;
                StringBuilder potholeDetails = new StringBuilder();

                for (DataSnapshot data : snapshot.getChildren()) {
                    totalPotholes++;

                    // Lấy thông tin địa chỉ từ database
                    String address = data.child("address").getValue(String.class);

                    // Thêm STT và địa chỉ vào danh sách hiển thị
                    potholeDetails.append(totalPotholes).append(". ").append(address).append("\n");
                }

                // Hiển thị tổng số pothole
                Total.setText("Total Potholes: " + totalPotholes);

                // Hiển thị danh sách pothole
                multilinePotholes.setText(potholeDetails.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Error fetching potholes data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}