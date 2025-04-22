package com.example.detectapplication2;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.util.Log;

public class WeatherFragment extends Fragment implements LocationListener {
    // OpenWeatherMap API Key
    private final String API_KEY = "90ec80953b809a18c31b89f696cf4b76";
    
    // Location related variables
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long MIN_TIME_BW_UPDATES = 5000; // 5 seconds
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    private boolean isLocationRequested = false;

    // UI Elements
    private TextView locationText;
    private EditText cityInput;
    private Button searchButton;
    private TextView temperatureText;
    private TextView conditionText;
    private TextView humidityText;
    private TextView windText;
    private TextView visibilityText;
    private TextView alertText;
    private CardView alertContainer;
    private TextView recommendation1;
    private TextView recommendation2;
    private TextView recommendation3;
    private ImageView weatherIcon;
    private TextView errorMessage;
    private LinearLayout hourlyForecastContainer;
    private CardView loadingIndicator; // Loading indicator
    
    // Forecast UI
    private TextView day1, day2, day3, day4, day5;
    private TextView day1Temp, day2Temp, day3Temp, day4Temp, day5Temp;
    private TextView day1Condition, day2Condition, day3Condition, day4Condition, day5Condition;

    public WeatherFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        // Initialize UI elements
        locationText = view.findViewById(R.id.locationText);
        cityInput = view.findViewById(R.id.cityInput);
        searchButton = view.findViewById(R.id.searchButton);
        temperatureText = view.findViewById(R.id.temperatureText);
        conditionText = view.findViewById(R.id.conditionText);
        humidityText = view.findViewById(R.id.humidityText);
        windText = view.findViewById(R.id.windText);
        visibilityText = view.findViewById(R.id.visibilityText);
        alertText = view.findViewById(R.id.alertText);
        alertContainer = view.findViewById(R.id.alertContainer);
        recommendation1 = view.findViewById(R.id.recommendation1);
        recommendation2 = view.findViewById(R.id.recommendation2);
        recommendation3 = view.findViewById(R.id.recommendation3);
        weatherIcon = view.findViewById(R.id.weatherIcon);
        errorMessage = view.findViewById(R.id.errorMessage);
        hourlyForecastContainer = view.findViewById(R.id.hourlyForecastContainer);
        loadingIndicator = view.findViewById(R.id.loadingIndicator); // Initialize loading indicator
        
        // Clear example hourly forecast (we'll populate it programmatically)
        hourlyForecastContainer.removeAllViews();
        
        // Forecast UI initialization
        day1 = view.findViewById(R.id.day1);
        day2 = view.findViewById(R.id.day2);
        day3 = view.findViewById(R.id.day3);
        day4 = view.findViewById(R.id.day4);
        day5 = view.findViewById(R.id.day5);
        day1Temp = view.findViewById(R.id.day1Temp);
        day2Temp = view.findViewById(R.id.day2Temp);
        day3Temp = view.findViewById(R.id.day3Temp);
        day4Temp = view.findViewById(R.id.day4Temp);
        day5Temp = view.findViewById(R.id.day5Temp);
        day1Condition = view.findViewById(R.id.day1Condition);
        day2Condition = view.findViewById(R.id.day2Condition);
        day3Condition = view.findViewById(R.id.day3Condition);
        day4Condition = view.findViewById(R.id.day4Condition);
        day5Condition = view.findViewById(R.id.day5Condition);
        
        // Set up days of the week
        setupDayLabels();

        // Set up search button click listener
        searchButton.setOnClickListener(v -> {
            String city = cityInput.getText().toString().trim();
            if (!city.isEmpty()) {
                // Hide any previous error message
                errorMessage.setVisibility(View.GONE);
                
                // Show loading indicator
                showLoading();
                
                // Pre-process city name to handle missing spaces
                city = processCityName(city);
                new GetWeatherTask(city).execute();
                new GetForecastTask(city).execute();
                new GetHourlyForecastTask(city).execute();
            } else {
                Toast.makeText(getActivity(), "Please enter a city name", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize location manager
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        
        // Ask for location permission when fragment is created
        requestLocationPermission();

        // DEBUG: Add a debug message to ensure we're using the real current time
        Calendar debugTime = Calendar.getInstance();
        int debugHour = debugTime.get(Calendar.HOUR_OF_DAY);
        int debugMin = debugTime.get(Calendar.MINUTE);
        Log.d("WeatherApp", "App started at current time: " + debugHour + ":" + debugMin);

        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // If user has already granted permission but location hasn't been requested yet
        if (hasLocationPermission() && !isLocationRequested) {
            getLocationWeather();
        } else if (!isLocationRequested) {
            // Default to Binh Duong if no location and not already requested
            new GetWeatherTask("Binh Duong").execute();
            new GetForecastTask("Binh Duong").execute();
            new GetHourlyForecastTask("Binh Duong").execute();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Stop location updates when paused
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
    
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestLocationPermission() {
        if (hasLocationPermission()) {
            // Already have permission, get location
            getLocationWeather();
            return;
        }
        
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Show explanation dialog
            new AlertDialog.Builder(getActivity())
                    .setTitle("Cần quyền truy cập vị trí")
                    .setMessage("Ứng dụng cần quyền truy cập vị trí để hiển thị thời tiết tại vị trí của bạn.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Request permission
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Không", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Load default weather
                            new GetWeatherTask("Binh Duong").execute();
                            new GetForecastTask("Binh Duong").execute();
                            new GetHourlyForecastTask("Binh Duong").execute();
                            isLocationRequested = true;
                        }
                    })
                    .create()
                    .show();
        } else {
            // First time asking, request directly
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            isLocationRequested = true;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                getLocationWeather();
            } else {
                // Permission denied
                Toast.makeText(getActivity(), "Sử dụng vị trí Bình Dương làm mặc định", Toast.LENGTH_SHORT).show();
                new GetWeatherTask("Binh Duong").execute();
                new GetForecastTask("Binh Duong").execute();
                new GetHourlyForecastTask("Binh Duong").execute();
            }
        }
    }
    
    private void getLocationWeather() {
        if (getActivity() == null) return;
        
        showLoading(); // Show loading indicator
        
        if (hasLocationPermission()) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                
                try {
                    // Request location updates
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    
                    // Also try network provider
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    
                    // Try to get last known location first
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation == null) {
                        lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    
                    if (lastKnownLocation != null) {
                        onLocationChanged(lastKnownLocation);
                    } else {
                        // If no last known location, wait for location update
                        Toast.makeText(getActivity(), "Đang tìm vị trí của bạn...", Toast.LENGTH_SHORT).show();
                    }
                    
                    isLocationRequested = true;
                } catch (SecurityException e) {
                    e.printStackTrace();
                    // Default to Binh Duong
                    new GetWeatherTask("Binh Duong").execute();
                    new GetForecastTask("Binh Duong").execute();
                    new GetHourlyForecastTask("Binh Duong").execute();
                }
            } else {
                // Location services disabled
                new AlertDialog.Builder(getActivity())
                        .setTitle("Dịch vụ vị trí đã tắt")
                        .setMessage("Vui lòng bật dịch vụ vị trí để xem thời tiết tại vị trí của bạn.")
                        .setPositiveButton("Mở cài đặt", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Open location settings
                                //startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                Toast.makeText(getActivity(), "Sử dụng vị trí Bình Dương làm mặc định", Toast.LENGTH_SHORT).show();
                                new GetWeatherTask("Binh Duong").execute();
                                new GetForecastTask("Binh Duong").execute();
                                new GetHourlyForecastTask("Binh Duong").execute();
                            }
                        })
                        .setNegativeButton("Không", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Default to Binh Duong
                                Toast.makeText(getActivity(), "Sử dụng vị trí Bình Dương làm mặc định", Toast.LENGTH_SHORT).show();
                                new GetWeatherTask("Binh Duong").execute();
                                new GetForecastTask("Binh Duong").execute();
                                new GetHourlyForecastTask("Binh Duong").execute();
                            }
                        })
                        .create()
                        .show();
            }
        } else {
            // No permission
            new GetWeatherTask("Binh Duong").execute();
            new GetForecastTask("Binh Duong").execute();
            new GetHourlyForecastTask("Binh Duong").execute();
        }
    }
    
    @Override
    public void onLocationChanged(Location location) {
        // Get weather for current location
        getWeatherFromLocation(location.getLatitude(), location.getLongitude());
    }
    
    private void getWeatherFromLocation(double latitude, double longitude) {
        showLoading(); // Show loading indicator
        
        // Get city name from coordinates
        Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String cityName = address.getLocality(); // city name
                
                if (cityName == null || cityName.isEmpty()) {
                    // Try with admin area if locality is not available
                    cityName = address.getAdminArea();
                }
                
                if (cityName != null && !cityName.isEmpty()) {
                    // Use city name from geocoder
                    new GetWeatherTask(cityName).execute();
                    new GetForecastTask(cityName).execute();
                    new GetHourlyForecastTask(cityName).execute();
                } else {
                    // Use coordinates directly
                    new GetWeatherByCoordinatesTask(latitude, longitude).execute();
                    new GetForecastByCoordinatesTask(latitude, longitude).execute();
                    new GetHourlyForecastByCoordinatesTask(latitude, longitude).execute();
                }
            } else {
                // Use coordinates directly if geocoder fails
                new GetWeatherByCoordinatesTask(latitude, longitude).execute();
                new GetForecastByCoordinatesTask(latitude, longitude).execute();
                new GetHourlyForecastByCoordinatesTask(latitude, longitude).execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Use coordinates directly if geocoder throws exception
            new GetWeatherByCoordinatesTask(latitude, longitude).execute();
            new GetForecastByCoordinatesTask(latitude, longitude).execute();
            new GetHourlyForecastByCoordinatesTask(latitude, longitude).execute();
        }
    }

    // Class to get weather by coordinates
    private class GetWeatherByCoordinatesTask extends AsyncTask<Void, Void, String> {
        private double latitude;
        private double longitude;

        public GetWeatherByCoordinatesTask(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoading();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String response = "";
            try {
                URL url = new URL("https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + 
                        "&lon=" + longitude + "&appid=" + API_KEY + "&units=metric");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                // Check if the request was successful
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return null;
                }
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response += line;
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                if (getActivity() != null) {
                    // Show error message and fall back to Binh Duong
                    Toast.makeText(getActivity(), "Không thể lấy dữ liệu thời tiết cho vị trí của bạn", Toast.LENGTH_SHORT).show();
                    new GetWeatherTask("Binh Duong").execute();
                }
                
                hideLoading(); // Hide loading on error
                return;
            }
            
            try {
                // Hide error message if it's visible
                errorMessage.setVisibility(View.GONE);
                
                JSONObject jsonObject = new JSONObject(result);
                JSONObject main = jsonObject.getJSONObject("main");
                double temp = main.getDouble("temp");
                int humidity = main.getInt("humidity");
                int visibility = jsonObject.getInt("visibility") / 1000; // Convert to km

                JSONObject wind = jsonObject.getJSONObject("wind");
                double windSpeed = wind.getDouble("speed");

                String weatherCondition = jsonObject.getJSONArray("weather")
                        .getJSONObject(0).getString("description");
                String weatherMain = jsonObject.getJSONArray("weather")
                        .getJSONObject(0).getString("main");
                String cityName = jsonObject.getString("name");
                String countryCode = jsonObject.getJSONObject("sys").getString("country");

                // Update UI with weather information
                locationText.setText(cityName + ", " + countryCode);
                temperatureText.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
                conditionText.setText(capitalizeFirstLetter(weatherCondition));
                humidityText.setText(humidity + "%");
                windText.setText(String.format(Locale.getDefault(), "%.1f km/h", windSpeed));
                visibilityText.setText(visibility + " km");
                
                // Update current weather title
                if (getView() != null) {
                    TextView currentWeatherTitle = getView().findViewById(R.id.currentWeatherTitle);
                    currentWeatherTitle.setText("Current Weather in " + cityName);
                }

                // Update weather icon based on conditions
                updateWeatherIcon(weatherMain, weatherCondition, temp);

                // Update driving alerts based on weather conditions
                updateDrivingAlert(weatherCondition, temp, humidity, windSpeed, visibility);

                // Hide loading at the end if all API calls completed
                hideLoading();
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    // Show error message
                    errorMessage.setVisibility(View.VISIBLE);
                    errorMessage.setText("Error processing weather data. Please try again.");
                }
                
                hideLoading(); // Hide loading on error
            }
        }
    }
    
    // Class to get forecast by coordinates
    private class GetForecastByCoordinatesTask extends AsyncTask<Void, Void, String> {
        private double latitude;
        private double longitude;

        public GetForecastByCoordinatesTask(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoading();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String response = "";
            try {
                URL url = new URL("https://api.openweathermap.org/data/2.5/forecast?lat=" + latitude + 
                        "&lon=" + longitude + "&appid=" + API_KEY + "&units=metric");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                // Check if the request was successful
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return null;
                }
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response += line;
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                hideLoading();
                return;
            }
            
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray list = jsonObject.getJSONArray("list");
                
                // We need to extract daily forecasts (1 forecast per day)
                // Get forecasts for next 5 days at noon if possible
                Map<String, Integer> dailyIndices = findDailyForecasts(list);
                
                // Update UI with daily forecasts
                if (dailyIndices.containsKey("tomorrow")) {
                    updateForecastDay(list, dailyIndices.get("tomorrow"), day1Temp, day1Condition);
                }
                
                if (dailyIndices.containsKey("day2")) {
                    updateForecastDay(list, dailyIndices.get("day2"), day2Temp, day2Condition);
                }
                
                if (dailyIndices.containsKey("day3")) {
                    updateForecastDay(list, dailyIndices.get("day3"), day3Temp, day3Condition);
                }
                
                if (dailyIndices.containsKey("day4")) {
                    updateForecastDay(list, dailyIndices.get("day4"), day4Temp, day4Condition);
                }
                
                if (dailyIndices.containsKey("day5")) {
                    updateForecastDay(list, dailyIndices.get("day5"), day5Temp, day5Condition);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Error processing forecast data", Toast.LENGTH_SHORT).show();
                }
            } finally {
                hideLoading();
            }
        }
        
        // Helper method to find forecasts for each day
        private Map<String, Integer> findDailyForecasts(JSONArray list) {
            Map<String, Integer> dailyIndices = new HashMap<>();
            
            try {
                // Get current date
                Calendar calendar = Calendar.getInstance();
                int currentDay = calendar.get(Calendar.DAY_OF_YEAR);
                
                // Track days we've seen to avoid duplicates
                Set<Integer> processedDays = new HashSet<>();
                
                // Loop through all forecasts to find one per day
                for (int i = 0; i < list.length(); i++) {
                    JSONObject forecast = list.getJSONObject(i);
                    String dtTxt = forecast.getString("dt_txt");
                    
                    // Parse the date
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date date = format.parse(dtTxt);
                    Calendar forecastCal = Calendar.getInstance();
                    forecastCal.setTime(date);
                    
                    int forecastDay = forecastCal.get(Calendar.DAY_OF_YEAR);
                    int forecastHour = forecastCal.get(Calendar.HOUR_OF_DAY);
                    
                    // Skip today
                    if (forecastDay == currentDay) {
                        continue;
                    }
                    
                    // If we haven't processed this day yet
                    if (!processedDays.contains(forecastDay)) {
                        // Preferably choose forecasts near noon (12:00)
                        if (forecastHour >= 11 && forecastHour <= 14) {
                            // Add to our processed days
                            processedDays.add(forecastDay);
                            
                            // Determine which day this is (tomorrow, day2, etc.)
                            int dayOffset = (forecastDay - currentDay + 365) % 365;
                            if (dayOffset <= 7) { // Ensure it's within a week
                                if (dayOffset == 1) {
                                    dailyIndices.put("tomorrow", i);
                                } else if (dayOffset == 2) {
                                    dailyIndices.put("day2", i);
                                } else if (dayOffset == 3) {
                                    dailyIndices.put("day3", i);
                                } else if (dayOffset == 4) {
                                    dailyIndices.put("day4", i);
                                } else if (dayOffset == 5) {
                                    dailyIndices.put("day5", i);
                                }
                            }
                        }
                    }
                }
                
                // If we couldn't find noon forecasts, take any forecasts for days
                if (dailyIndices.size() < 5) {
                    processedDays.clear();
                    
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject forecast = list.getJSONObject(i);
                        String dtTxt = forecast.getString("dt_txt");
                        
                        // Parse the date
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        Date date = format.parse(dtTxt);
                        Calendar forecastCal = Calendar.getInstance();
                        forecastCal.setTime(date);
                        
                        int forecastDay = forecastCal.get(Calendar.DAY_OF_YEAR);
                        
                        // Skip today
                        if (forecastDay == currentDay) {
                            continue;
                        }
                        
                        // If we haven't processed this day yet
                        if (!processedDays.contains(forecastDay)) {
                            // Add to our processed days
                            processedDays.add(forecastDay);
                            
                            // Determine which day this is (tomorrow, day2, etc.)
                            int dayOffset = (forecastDay - currentDay + 365) % 365;
                            if (dayOffset <= 7) { // Ensure it's within a week
                                if (dayOffset == 1 && !dailyIndices.containsKey("tomorrow")) {
                                    dailyIndices.put("tomorrow", i);
                                } else if (dayOffset == 2 && !dailyIndices.containsKey("day2")) {
                                    dailyIndices.put("day2", i);
                                } else if (dayOffset == 3 && !dailyIndices.containsKey("day3")) {
                                    dailyIndices.put("day3", i);
                                } else if (dayOffset == 4 && !dailyIndices.containsKey("day4")) {
                                    dailyIndices.put("day4", i);
                                } else if (dayOffset == 5 && !dailyIndices.containsKey("day5")) {
                                    dailyIndices.put("day5", i);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return dailyIndices;
        }
        
        private void updateForecastDay(JSONArray list, int index, TextView tempView, TextView conditionView) {
            try {
                if (index < list.length()) {
                    JSONObject dayData = list.getJSONObject(index);
                    JSONObject main = dayData.getJSONObject("main");
                    double temp = main.getDouble("temp");
                    
                    String condition = dayData.getJSONArray("weather")
                            .getJSONObject(0).getString("description");
                    
                    tempView.setText(String.format(Locale.getDefault(), "%.0f°C", temp));
                    conditionView.setText(capitalizeFirstLetter(condition));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // Class to get hourly forecast by coordinates
    private class GetHourlyForecastByCoordinatesTask extends AsyncTask<Void, Void, String> {
        private double latitude;
        private double longitude;

        public GetHourlyForecastByCoordinatesTask(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoading();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String response = "";
            try {
                URL url = new URL("https://api.openweathermap.org/data/2.5/forecast?lat=" + latitude + 
                        "&lon=" + longitude + "&appid=" + API_KEY + "&units=metric");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                // Check if the request was successful
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return null;
                }
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response += line;
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                hideLoading();
                return;
            }
            
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray list = jsonObject.getJSONArray("list");
                
                // Clear previous hourly forecasts
                hourlyForecastContainer.removeAllViews();

                // Get current time for the "Now" item - Force update to ensure we have the latest time
                Calendar currentTime = Calendar.getInstance();
                
                // Log current time for debugging
                Log.d("WeatherApp", "Hourly forecast at time: " + currentTime.get(Calendar.HOUR_OF_DAY) + ":" + currentTime.get(Calendar.MINUTE));
                
                // Get current weather data for "Now"
                JSONObject currentWeatherData = list.getJSONObject(0);
                double currentTemp = currentWeatherData.getJSONObject("main").getDouble("temp");
                String currentWeatherMain = currentWeatherData.getJSONArray("weather").getJSONObject(0).getString("main");
                String currentWeatherDesc = currentWeatherData.getJSONArray("weather").getJSONObject(0).getString("description");
                
                // Add the "Now" item with current weather data
                addHourForecastItem("Now", currentTemp, currentWeatherMain, currentWeatherDesc);
                
                // Get current hour from the system time, not from the API data
                int currentHour = currentTime.get(Calendar.HOUR_OF_DAY);
                Log.d("WeatherApp", "Using current hour from system: " + currentHour);
                
                // Fixed offsets of 3-hour intervals from now 
                int[] hourOffsets = {3, 6, 9, 12, 15};
                
                // Update hourly forecast with correct increments from current time
                for (int i = 0; i < hourOffsets.length && i < list.length() - 1; i++) {
                    JSONObject forecastData = list.getJSONObject(i + 1);
                    double temp = forecastData.getJSONObject("main").getDouble("temp");
                    String weatherMain = forecastData.getJSONArray("weather").getJSONObject(0).getString("main");
                    String weatherDesc = forecastData.getJSONArray("weather").getJSONObject(0).getString("description");
                    
                    // Calculate exact hour based on current system time
                    int forecastHour = (currentHour + hourOffsets[i]) % 24;
                    String timeDisplay = String.format(Locale.getDefault(), "%02d:00", forecastHour);
                    Log.d("WeatherApp", "Displaying forecast for hour: " + timeDisplay);
                    
                    // Add the forecast item with the calculated hour
                    addHourForecastItem(timeDisplay, temp, weatherMain, weatherDesc);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("WeatherApp", "Error processing hourly forecast: " + e.getMessage());
            } finally {
                hideLoading();
            }
        }

        // Helper function to format the time as "HH:00"
        private String formatTime(int hour) {
            return String.format(Locale.getDefault(), "%02d:00", hour);
        }

    }

    private void setupDayLabels() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        
        // Skip today for the labels
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        day1.setText("Tomorrow");
        
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        day2.setText(dayFormat.format(calendar.getTime()));
        
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        day3.setText(dayFormat.format(calendar.getTime()));
        
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        day4.setText(dayFormat.format(calendar.getTime()));
        
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        day5.setText(dayFormat.format(calendar.getTime()));
    }

    private class GetWeatherTask extends AsyncTask<Void, Void, String> {
        private String city;

        public GetWeatherTask(String city) {
            this.city = city;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoading();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String response = "";
            try {
                URL url = new URL("https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY + "&units=metric");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                // Check if the request was successful
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return null;
                }
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response += line;
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                if (getActivity() != null) {
                    // Show error message
                    errorMessage.setVisibility(View.VISIBLE);
                    errorMessage.setText("City not found! Please check the name and try again.");
                }
                
                hideLoading(); // Hide loading on error
                return;
            }
            
            try {
                // Hide error message if it's visible
                errorMessage.setVisibility(View.GONE);
                
                JSONObject jsonObject = new JSONObject(result);
                JSONObject main = jsonObject.getJSONObject("main");
                double temp = main.getDouble("temp");
                int humidity = main.getInt("humidity");
                int visibility = jsonObject.getInt("visibility") / 1000; // Convert to km

                JSONObject wind = jsonObject.getJSONObject("wind");
                double windSpeed = wind.getDouble("speed");

                String weatherCondition = jsonObject.getJSONArray("weather")
                        .getJSONObject(0).getString("description");
                String weatherMain = jsonObject.getJSONArray("weather")
                        .getJSONObject(0).getString("main");
                String cityName = jsonObject.getString("name");
                String countryCode = jsonObject.getJSONObject("sys").getString("country");

                // Update UI with weather information
                locationText.setText(cityName + ", " + countryCode);
                temperatureText.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
                conditionText.setText(capitalizeFirstLetter(weatherCondition));
                humidityText.setText(humidity + "%");
                windText.setText(String.format(Locale.getDefault(), "%.1f km/h", windSpeed));
                visibilityText.setText(visibility + " km");
                
                // Update current weather title
                if (getView() != null) {
                    TextView currentWeatherTitle = getView().findViewById(R.id.currentWeatherTitle);
                    currentWeatherTitle.setText("Current Weather in " + cityName);
                }

                // Update weather icon based on conditions
                updateWeatherIcon(weatherMain, weatherCondition, temp);

                // Update driving alerts based on weather conditions
                updateDrivingAlert(weatherCondition, temp, humidity, windSpeed, visibility);

                // Hide loading at the end
                hideLoading();
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    // Show error message
                    errorMessage.setVisibility(View.VISIBLE);
                    errorMessage.setText("Error processing weather data. Please try again.");
                }
                
                hideLoading(); // Hide loading on error
            }
        }
    }
    
    private class GetHourlyForecastTask extends AsyncTask<Void, Void, String> {
        private String city;

        public GetHourlyForecastTask(String city) {
            this.city = city;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoading();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String response = "";
            try {
                URL url = new URL("https://api.openweathermap.org/data/2.5/forecast?q=" + city + "&appid=" + API_KEY + "&units=metric");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                // Check if the request was successful
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return null;
                }
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response += line;
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                hideLoading();
                return;
            }
            
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray list = jsonObject.getJSONArray("list");
                
                // Clear previous hourly forecasts
                hourlyForecastContainer.removeAllViews();

                // Get current time for the "Now" item - Force update to ensure we have the latest time
                Calendar currentTime = Calendar.getInstance();
                
                // Log current time for debugging
                Log.d("WeatherApp", "Hourly forecast at time: " + currentTime.get(Calendar.HOUR_OF_DAY) + ":" + currentTime.get(Calendar.MINUTE));
                
                // Get current weather data for "Now"
                JSONObject currentWeatherData = list.getJSONObject(0);
                double currentTemp = currentWeatherData.getJSONObject("main").getDouble("temp");
                String currentWeatherMain = currentWeatherData.getJSONArray("weather").getJSONObject(0).getString("main");
                String currentWeatherDesc = currentWeatherData.getJSONArray("weather").getJSONObject(0).getString("description");
                
                // Add the "Now" item with current weather data
                addHourForecastItem("Now", currentTemp, currentWeatherMain, currentWeatherDesc);
                
                // Get current hour from the system time, not from the API data
                int currentHour = currentTime.get(Calendar.HOUR_OF_DAY);
                Log.d("WeatherApp", "Using current hour from system: " + currentHour);
                
                // Fixed offsets of 3-hour intervals from now 
                int[] hourOffsets = {3, 6, 9, 12, 15};
                
                // Update hourly forecast with correct increments from current time
                for (int i = 0; i < hourOffsets.length && i < list.length() - 1; i++) {
                    JSONObject forecastData = list.getJSONObject(i + 1);
                    double temp = forecastData.getJSONObject("main").getDouble("temp");
                    String weatherMain = forecastData.getJSONArray("weather").getJSONObject(0).getString("main");
                    String weatherDesc = forecastData.getJSONArray("weather").getJSONObject(0).getString("description");
                    
                    // Calculate exact hour based on current system time
                    int forecastHour = (currentHour + hourOffsets[i]) % 24;
                    String timeDisplay = String.format(Locale.getDefault(), "%02d:00", forecastHour);
                    Log.d("WeatherApp", "Displaying forecast for hour: " + timeDisplay);
                    
                    // Add the forecast item with the calculated hour
                    addHourForecastItem(timeDisplay, temp, weatherMain, weatherDesc);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("WeatherApp", "Error processing hourly forecast: " + e.getMessage());
            } finally {
                hideLoading();
            }
        }
    }
    
    private void addHourForecastItem(String time, double temperature, String weatherMain, String weatherDesc) {
        // Skip if container is not available
        if (hourlyForecastContainer == null || getContext() == null) return;
        
        // Inflate the hourly forecast item view from XML layout
        View itemView = getLayoutInflater().inflate(R.layout.hourly_forecast_item, hourlyForecastContainer, false);
        
        // Get references to views in the layout
        TextView timeText = itemView.findViewById(R.id.timeText);
        ImageView icon = itemView.findViewById(R.id.weatherIcon);
        TextView tempText = itemView.findViewById(R.id.tempText);
        
        // Set the data
        timeText.setText(time);
        tempText.setText(String.format(Locale.getDefault(), "%.0f°C", temperature));
        
        // Set icon based on weather
        if (weatherMain.equalsIgnoreCase("Clear")) {
            icon.setImageResource(R.drawable.icon_sunny);
        } 
        else if (weatherMain.equalsIgnoreCase("Clouds")) {
            icon.setImageResource(R.drawable.icon_cloudy);
        } 
        else if (weatherMain.equalsIgnoreCase("Rain") || 
                 weatherMain.equalsIgnoreCase("Drizzle") ||
                 weatherDesc.contains("rain")) {
            icon.setImageResource(R.drawable.icon_rainy);
        } 
        else if (weatherMain.equalsIgnoreCase("Thunderstorm") ||
                 weatherDesc.contains("thunder")) {
            icon.setImageResource(R.drawable.icon_thunderstorm);
        } 
        else if (weatherMain.equalsIgnoreCase("Snow") || 
                 weatherDesc.contains("snow") || 
                 weatherDesc.contains("sleet") || 
                 temperature < 0) {
            icon.setImageResource(R.drawable.icon_snow);
        } 
        else if (weatherMain.equalsIgnoreCase("Mist") || 
                 weatherMain.equalsIgnoreCase("Fog") || 
                 weatherMain.equalsIgnoreCase("Haze") || 
                 weatherDesc.contains("fog")) {
            icon.setImageResource(R.drawable.icon_fog);
        } else {
            icon.setImageResource(R.drawable.icon_sunny);
        }
        
        // Add the item to the container
        hourlyForecastContainer.addView(itemView);
    }
    
    private String formatTimeFromTimestamp(String timestamp) {
        try {
            // Input format: "2023-06-03 12:00:00"
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            
            // Extract just the hour for cleaner display
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return String.format(Locale.getDefault(), "%02d:00", cal.get(Calendar.HOUR_OF_DAY)); 
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: extract hour only from the timestamp
            return timestamp.substring(11, 13) + ":00"; 
        }
    }
    
    private class GetForecastTask extends AsyncTask<Void, Void, String> {
        private String city;

        public GetForecastTask(String city) {
            this.city = city;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoading();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String response = "";
            try {
                URL url = new URL("https://api.openweathermap.org/data/2.5/forecast?q=" + city + "&appid=" + API_KEY + "&units=metric");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                // Check if the request was successful
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return null;
                }
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response += line;
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                hideLoading();
                return;
            }
            
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray list = jsonObject.getJSONArray("list");
                
                // We need to extract daily forecasts (1 forecast per day)
                // Get forecasts for next 5 days at noon if possible
                Map<String, Integer> dailyIndices = findDailyForecasts(list);
                
                // Update UI with daily forecasts
                if (dailyIndices.containsKey("tomorrow")) {
                    updateForecastDay(list, dailyIndices.get("tomorrow"), day1Temp, day1Condition);
                }
                
                if (dailyIndices.containsKey("day2")) {
                    updateForecastDay(list, dailyIndices.get("day2"), day2Temp, day2Condition);
                }
                
                if (dailyIndices.containsKey("day3")) {
                    updateForecastDay(list, dailyIndices.get("day3"), day3Temp, day3Condition);
                }
                
                if (dailyIndices.containsKey("day4")) {
                    updateForecastDay(list, dailyIndices.get("day4"), day4Temp, day4Condition);
                }
                
                if (dailyIndices.containsKey("day5")) {
                    updateForecastDay(list, dailyIndices.get("day5"), day5Temp, day5Condition);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Error processing forecast data", Toast.LENGTH_SHORT).show();
                }
            } finally {
                hideLoading();
            }
        }
        
        // Helper method to find forecasts for each day
        private Map<String, Integer> findDailyForecasts(JSONArray list) {
            Map<String, Integer> dailyIndices = new HashMap<>();
            
            try {
                // Get current date
                Calendar calendar = Calendar.getInstance();
                int currentDay = calendar.get(Calendar.DAY_OF_YEAR);
                
                // Track days we've seen to avoid duplicates
                Set<Integer> processedDays = new HashSet<>();
                
                // Loop through all forecasts to find one per day
                for (int i = 0; i < list.length(); i++) {
                    JSONObject forecast = list.getJSONObject(i);
                    String dtTxt = forecast.getString("dt_txt");
                    
                    // Parse the date
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date date = format.parse(dtTxt);
                    Calendar forecastCal = Calendar.getInstance();
                    forecastCal.setTime(date);
                    
                    int forecastDay = forecastCal.get(Calendar.DAY_OF_YEAR);
                    int forecastHour = forecastCal.get(Calendar.HOUR_OF_DAY);
                    
                    // Skip today
                    if (forecastDay == currentDay) {
                        continue;
                    }
                    
                    // If we haven't processed this day yet
                    if (!processedDays.contains(forecastDay)) {
                        // Preferably choose forecasts near noon (12:00)
                        if (forecastHour >= 11 && forecastHour <= 14) {
                            // Add to our processed days
                            processedDays.add(forecastDay);
                            
                            // Determine which day this is (tomorrow, day2, etc.)
                            int dayOffset = (forecastDay - currentDay + 365) % 365;
                            if (dayOffset <= 7) { // Ensure it's within a week
                                if (dayOffset == 1) {
                                    dailyIndices.put("tomorrow", i);
                                } else if (dayOffset == 2) {
                                    dailyIndices.put("day2", i);
                                } else if (dayOffset == 3) {
                                    dailyIndices.put("day3", i);
                                } else if (dayOffset == 4) {
                                    dailyIndices.put("day4", i);
                                } else if (dayOffset == 5) {
                                    dailyIndices.put("day5", i);
                                }
                            }
                        }
                    }
                }
                
                // If we couldn't find noon forecasts, take any forecasts for days
                if (dailyIndices.size() < 5) {
                    processedDays.clear();
                    
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject forecast = list.getJSONObject(i);
                        String dtTxt = forecast.getString("dt_txt");
                        
                        // Parse the date
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        Date date = format.parse(dtTxt);
                        Calendar forecastCal = Calendar.getInstance();
                        forecastCal.setTime(date);
                        
                        int forecastDay = forecastCal.get(Calendar.DAY_OF_YEAR);
                        
                        // Skip today
                        if (forecastDay == currentDay) {
                            continue;
                        }
                        
                        // If we haven't processed this day yet
                        if (!processedDays.contains(forecastDay)) {
                            // Add to our processed days
                            processedDays.add(forecastDay);
                            
                            // Determine which day this is (tomorrow, day2, etc.)
                            int dayOffset = (forecastDay - currentDay + 365) % 365;
                            if (dayOffset <= 7) { // Ensure it's within a week
                                if (dayOffset == 1 && !dailyIndices.containsKey("tomorrow")) {
                                    dailyIndices.put("tomorrow", i);
                                } else if (dayOffset == 2 && !dailyIndices.containsKey("day2")) {
                                    dailyIndices.put("day2", i);
                                } else if (dayOffset == 3 && !dailyIndices.containsKey("day3")) {
                                    dailyIndices.put("day3", i);
                                } else if (dayOffset == 4 && !dailyIndices.containsKey("day4")) {
                                    dailyIndices.put("day4", i);
                                } else if (dayOffset == 5 && !dailyIndices.containsKey("day5")) {
                                    dailyIndices.put("day5", i);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return dailyIndices;
        }
        
        private void updateForecastDay(JSONArray list, int index, TextView tempView, TextView conditionView) {
            try {
                if (index < list.length()) {
                    JSONObject dayData = list.getJSONObject(index);
                    JSONObject main = dayData.getJSONObject("main");
                    double temp = main.getDouble("temp");
                    
                    String condition = dayData.getJSONArray("weather")
                            .getJSONObject(0).getString("description");
                    
                    tempView.setText(String.format(Locale.getDefault(), "%.0f°C", temp));
                    conditionView.setText(capitalizeFirstLetter(condition));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Update the weather icon based on current weather conditions
     */
    private void updateWeatherIcon(String weatherMain, String weatherDescription, double temperature) {
        if (weatherIcon == null) return;

        // Default icon
        int iconResource = R.drawable.icon_sunny;

        // Check weather conditions to determine the appropriate icon
        if (weatherMain.equalsIgnoreCase("Clear")) {
            iconResource = R.drawable.icon_sunny;
        } 
        else if (weatherMain.equalsIgnoreCase("Clouds")) {
            iconResource = R.drawable.icon_cloudy;
        } 
        else if (weatherMain.equalsIgnoreCase("Rain") || 
                 weatherMain.equalsIgnoreCase("Drizzle") ||
                 weatherDescription.contains("rain")) {
            iconResource = R.drawable.icon_rainy;
        } 
        else if (weatherMain.equalsIgnoreCase("Thunderstorm") ||
                 weatherDescription.contains("thunder")) {
            iconResource = R.drawable.icon_thunderstorm;
        } 
        else if (weatherMain.equalsIgnoreCase("Snow") || 
                 weatherDescription.contains("snow") || 
                 weatherDescription.contains("sleet") || 
                 temperature < 0) {
            iconResource = R.drawable.icon_snow;
        } 
        else if (weatherMain.equalsIgnoreCase("Mist") || 
                 weatherMain.equalsIgnoreCase("Fog") || 
                 weatherMain.equalsIgnoreCase("Haze") || 
                 weatherDescription.contains("fog")) {
            iconResource = R.drawable.icon_fog;
        }

        // Set the icon
        weatherIcon.setImageResource(iconResource);
    }

    private void updateDrivingAlert(String weatherCondition, double temperature, int humidity, double windSpeed, int visibility) {
        String alertMessage;
        String recommendation1Text = "• Maintain safe following distance";
        String recommendation2Text = "• Check tire pressure before long trips";
        String recommendation3Text = "• Drive at moderate speeds";
        int alertColor = 0xFFFFF8E1; // Default light yellow

        // Check for hazardous conditions
        if (weatherCondition.contains("rain") || weatherCondition.contains("drizzle")) {
            alertMessage = "Rainy conditions. Reduce speed and increase following distance. Roads may be slippery.";
            recommendation1Text = "• Use headlights even during daytime";
            recommendation2Text = "• Avoid sudden braking or turns";
            recommendation3Text = "• Keep windshield wipers in good condition";
            alertColor = 0xFFE1F5FE; // Light blue
        } else if (weatherCondition.contains("thunderstorm")) {
            alertMessage = "Severe thunderstorm. Avoid driving if possible. Find shelter if driving conditions worsen.";
            recommendation1Text = "• Stay away from flooded areas";
            recommendation2Text = "• Be cautious of fallen debris and trees";
            recommendation3Text = "• Pull over safely if visibility is severely reduced";
            alertColor = 0xFFFFCDD2; // Light red
        } else if (weatherCondition.contains("snow") || weatherCondition.contains("sleet")) {
            alertMessage = "Snowy conditions. Drive slowly and cautiously. Roads are slippery with reduced traction.";
            recommendation1Text = "• Keep greater distance from other vehicles";
            recommendation2Text = "• Accelerate and decelerate slowly";
            recommendation3Text = "• Avoid stopping if possible, especially uphill";
            alertColor = 0xFFE1F5FE; // Light blue
        } else if (weatherCondition.contains("fog") || visibility < 5) {
            alertMessage = "Foggy conditions with reduced visibility. Use fog lights and drive slowly.";
            recommendation1Text = "• Use low beam headlights";
            recommendation2Text = "• Follow road markings carefully";
            recommendation3Text = "• Avoid overtaking other vehicles";
            alertColor = 0xFFE0E0E0; // Light gray
        } else if (windSpeed > 20) {
            alertMessage = "Strong winds. Be cautious of gusts affecting vehicle stability, especially on bridges.";
            recommendation1Text = "• Keep both hands on the steering wheel";
            recommendation2Text = "• Be careful when passing large vehicles";
            recommendation3Text = "• Watch for falling debris on the road";
            alertColor = 0xFFE0E0E0; // Light gray
        } else if (temperature > 35) {
            alertMessage = "Very hot conditions. Stay hydrated and check vehicle cooling systems. Watch for tire issues.";
            recommendation1Text = "• Check tire pressure (heat increases pressure)";
            recommendation2Text = "• Ensure AC and cooling systems work properly";
            recommendation3Text = "• Bring water for longer journeys";
            alertColor = 0xFFFFECB3; // Light amber
        } else if (temperature < 5) {
            alertMessage = "Cold conditions. Watch for icy patches, especially on bridges and in shaded areas.";
            recommendation1Text = "• Check tire tread and pressure";
            recommendation2Text = "• Keep fuel tank at least half full";
            recommendation3Text = "• Carry emergency supplies";
            alertColor = 0xFFE1F5FE; // Light blue
        } else {
            alertMessage = "Good conditions for driving. Stay hydrated and drive safely.";
            alertColor = 0xFFF1F8E9; // Light green
        }

        // Update UI with alert and recommendations
        alertText.setText(alertMessage);
        recommendation1.setText(recommendation1Text);
        recommendation2.setText(recommendation2Text);
        recommendation3.setText(recommendation3Text);
        alertContainer.setCardBackgroundColor(alertColor);
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /**
     * Process city name input to handle common formatting issues
     * For example: "BinhDuong" -> "Binh Duong", "HoChiMinh" -> "Ho Chi Minh"
     */
    private String processCityName(String input) {
        // Common Vietnamese city names that might be typed without spaces
        String[][] cityPairs = {
            {"BinhDuong", "Binh Duong"},
            {"HoChiMinh", "Ho Chi Minh"},
            {"HoChiMinhCity", "Ho Chi Minh City"},
            {"HaNoi", "Ha Noi"},
            {"DaNang", "Da Nang"},
            {"CanTho", "Can Tho"},
            {"VungTau", "Vung Tau"},
            {"NhaTrang", "Nha Trang"},
            {"HaiPhong", "Hai Phong"},
            {"QuiNhon", "Qui Nhon"},
            {"BienHoa", "Bien Hoa"},
            {"BuonMaThuot", "Buon Ma Thuot"},
            {"HueCityVietnam", "Hue"},
            {"HueCity", "Hue"},
            {"Hue", "Hue"}
        };
        
        // Check if input matches any known city without spaces
        for (String[] pair : cityPairs) {
            if (input.equalsIgnoreCase(pair[0])) {
                return pair[1];
            }
        }
        
        // If no match found, try to add spaces before capital letters
        // This works for camelCase city names like "HoChiMinh"
        if (input.length() > 2) {
            StringBuilder processed = new StringBuilder();
            processed.append(input.charAt(0));
            
            for (int i = 1; i < input.length(); i++) {
                char c = input.charAt(i);
                // If this is an uppercase letter and the previous character isn't a space
                if (Character.isUpperCase(c) && input.charAt(i-1) != ' ') {
                    processed.append(' ').append(c);
                } else {
                    processed.append(c);
                }
            }
            
            return processed.toString();
        }
        
        // Return original if no processing needed
        return input;
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Required for LocationListener interface
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Required for LocationListener interface
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Required for LocationListener interface but deprecated in newer APIs
    }

    private void showLoading() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.GONE);
        }
    }
} 