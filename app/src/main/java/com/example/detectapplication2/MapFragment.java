package com.example.detectapplication2;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.here.sdk.core.Color;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.Point2D;
import com.here.sdk.core.engine.SDKNativeEngine;
import com.here.sdk.core.engine.SDKOptions;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapview.LineCap;
import com.here.sdk.mapview.MapImage;
import com.here.sdk.mapview.MapImageFactory;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapMeasure;
import com.here.sdk.mapview.MapMeasureDependentRenderSize;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import com.here.sdk.mapview.RenderSize;
import com.here.sdk.routing.CarOptions;
import com.here.sdk.routing.Maneuver;
import com.here.sdk.routing.ManeuverAction;
import com.here.sdk.routing.PaymentMethod;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RouteOptions;
import com.here.sdk.routing.RouteRailwayCrossing;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.Section;
import com.here.sdk.routing.Span;
import com.here.sdk.routing.Toll;
import com.here.sdk.routing.TollFare;
import com.here.sdk.routing.TrafficOptimizationMode;
import com.here.sdk.routing.Waypoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {
    private static final String TAG = MapFragment.class.getSimpleName();
    private static final String SEARCH_API_KEY = "A_OO6C4HDauxRb4gKLjXJjhDctyz2CV0EYZqogOh3rs";
    
    // UI elements
    private ListView searchResultsList;
    private EditText locationSearch;
    private Button searchButton;
    private TextView estimatedTimeTextView;
    private ViewStub mapStub;
    
    // Map data
    private MapView mapView;
    private boolean isMapViewInitialized = false;
    private boolean isMapViewInflated = false;
    private GeoCoordinates currentLocation = null;
    private GeoCoordinates previousLocation = null;
    private GeoCoordinates destinationCoordinates;
    private MapMarker currentLocationMarker;
    private final List<MapMarker> searchMarkers = new ArrayList<>();
    private final List<MapPolyline> mapPolylines = new ArrayList<>();
    private List<Pothole> potholesOnRoute = new ArrayList<>();
    private final List<String> searchResultsTitles = new ArrayList<>();
    private final List<GeoCoordinates> searchResultsCoordinates = new ArrayList<>();
    
    // Location services
    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    
    // Routing
    private RoutingEngine routingEngine;
    private boolean trafficDisabled = false;
    
    // Notification
    private Handler notificationHandler;
    private Runnable notificationRunnable;
    
    // Fragment state tracking
    private boolean isFragmentActive = false;
    private volatile boolean isSDKInitialized = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Initialize the SDK and routing engine only once
            if (!isSDKInitialized) {
                initializeHERESDK();
            }
            
            if (routingEngine == null) {
                initializeRoutingEngine();
            }
            
            // Create location callback
            createLocationCallback();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        
        try {
            // Initialize UI elements
            searchResultsList = view.findViewById(R.id.search_results_list);
            locationSearch = view.findViewById(R.id.location_search);
            searchButton = view.findViewById(R.id.search_button);
            estimatedTimeTextView = view.findViewById(R.id.estimated_time);
            mapStub = view.findViewById(R.id.map_stub);
            
            // Set click listeners
            searchButton.setOnClickListener(v -> {
                String query = locationSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                } else {
                    showToast("Please enter a location to search.");
                }
            });
            
            searchResultsList.setOnItemClickListener((parent, view1, position, id) -> {
                if (position < 0 || position >= searchResultsCoordinates.size()) return;
                
                GeoCoordinates selectedCoordinates = searchResultsCoordinates.get(position);
                destinationCoordinates = selectedCoordinates;
                updateMapLocation(selectedCoordinates);
                searchResultsList.setVisibility(View.GONE);
                
                if (mapView != null) {
                    clearPolylines();
                    clearSearchMarkers();
                }
                
                if (currentLocation != null) {
                    calculateRoute(currentLocation, selectedCoordinates);
                } else {
                    showToast("Current location not available");
                }
            });
            
            // Initialize location services if available
            if (getActivity() != null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
            }
            
            // We don't inflate the map here - we'll do it in onViewCreated
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage());
        }
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isFragmentActive = true;
        
        // We will inflate the map when the fragment is visible to the user
        // this significantly reduces memory usage and prevents crashes
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        
        // Inflate map when fragment becomes visible
        if (!isMapViewInflated && mapStub != null) {
            inflateMapView();
        } else if (mapView != null) {
            // Resume map if already inflated
            mapView.onResume();
            startLocationUpdates();
        }
    }
    
    private void inflateMapView() {
        try {
            // Inflate the map view from the ViewStub
            View inflated = mapStub.inflate();
            mapView = inflated.findViewById(R.id.map_view);
            isMapViewInflated = true;
            
            if (mapView != null) {
                // Initialize the map
                mapView.onCreate(null);
                
                // Load the map scene with a 300ms delay to prevent UI jank
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isFragmentActive) {
                        loadMapScene();
                    }
                }, 300);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inflating map view: " + e.getMessage());
            showToast("Failed to load map. Please try again.");
        }
    }

    private void initializeHERESDK() {
        try {
            // Skip if SDK is already initialized
            if (SDKNativeEngine.getSharedInstance() != null) {
                isSDKInitialized = true;
                return;
            }
            
            String accessKeyId = "YjeF7IVNwWjHzNEoS-ckFg";
            String accessKeySecret = "sq60IU-QfsqLgzFpv0gsDz08NK7INHwwG6JpuJSek9fTXAYcBPBYYWZcc8915DtGBWeE5HXZ7TrbgbKXQDS7rw";
            SDKOptions options = new SDKOptions(accessKeyId, accessKeySecret);
            
            Context context = getContext();
            if (context == null) {
                Log.e(TAG, "Failed to initialize HERE SDK: Context is null");
                return;
            }
            
            SDKNativeEngine.makeSharedInstance(context, options);
            isSDKInitialized = true;
            Log.d(TAG, "HERE SDK initialized successfully");
        } catch (InstantiationErrorException e) {
            Log.e(TAG, "HERE SDK initialization failed: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error initializing HERE SDK: " + e.getMessage());
        }
    }

    private void initializeRoutingEngine() {
        try {
            if (isSDKInitialized) {
                routingEngine = new RoutingEngine();
            }
        } catch (InstantiationErrorException e) {
            Log.e(TAG, "Error initializing RoutingEngine: " + e.getMessage());
        }
    }
    
    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && isFragmentActive && mapView != null) {
                    currentLocation = new GeoCoordinates(location.getLatitude(), location.getLongitude());
                    updateLocationMarker(currentLocation);
                }
            }
        };
    }
    
    private void loadMapScene() {
        if (mapView == null || !isFragmentActive) return;
        
        mapView.getMapScene().loadScene(MapScheme.NORMAL_DAY, mapError -> {
            if (!isFragmentActive) return;
            
            if (mapError != null) {
                Log.e(TAG, "Failed to load map scene: " + mapError.name());
                showToast("Failed to load map");
            } else {
                isMapViewInitialized = true;
                Log.d(TAG, "Map scene loaded successfully");
                
                // Check permissions and get location
                handlePermissions();
            }
        });
    }
    
    private void handlePermissions() {
        if (getContext() == null || getActivity() == null) return;
        
        if (ActivityCompat.checkSelfPermission(getContext(), 
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            // Get current location and start updates
            requestCurrentLocation();
            startLocationUpdates();
        }
    }
    
    private void requestCurrentLocation() {
        if (getContext() == null || getActivity() == null || fusedLocationClient == null) return;
        
        if (ActivityCompat.checkSelfPermission(getContext(), 
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && isFragmentActive && mapView != null) {
                currentLocation = new GeoCoordinates(location.getLatitude(), location.getLongitude());
                moveCameraToCurrentLocation();
            }
        });
    }
    
    private void startLocationUpdates() {
        if (getContext() == null || getActivity() == null || fusedLocationClient == null) return;
        
        if (ActivityCompat.checkSelfPermission(getContext(), 
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000);
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }
    
    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
    
    private void moveCameraToCurrentLocation() {
        if (currentLocation == null || mapView == null || !isMapViewInitialized) return;
        
        MapMeasure mapMeasure = new MapMeasure(MapMeasure.Kind.DISTANCE, 1000);
        mapView.getCamera().lookAt(currentLocation, mapMeasure);
        
        // Add marker for current location
        updateLocationMarker(currentLocation);
    }
    
    private void updateLocationMarker(GeoCoordinates location) {
        if (mapView == null || !isFragmentActive || !isMapViewInitialized) return;
        
        try {
            // Remove existing marker
            if (currentLocationMarker != null) {
                mapView.getMapScene().removeMapMarker(currentLocationMarker);
            }
            
            // Create new marker
            MapImage markerImage = MapImageFactory.fromResource(getResources(), R.drawable.ic_current_location);
            currentLocationMarker = new MapMarker(location, markerImage);
            mapView.getMapScene().addMapMarker(currentLocationMarker);
        } catch (Exception e) {
            Log.e(TAG, "Error updating location marker: " + e.getMessage());
        }
    }
    
    private void updateMapLocation(GeoCoordinates coordinates) {
        if (coordinates == null || mapView == null || !isMapViewInitialized) return;
        
        try {
            // Clear existing markers
            clearSearchMarkers();
            
            // Move camera
            MapMeasure mapMeasure = new MapMeasure(MapMeasure.Kind.DISTANCE, 1000);
            mapView.getCamera().lookAt(coordinates, mapMeasure);
            
            // Add new marker
            MapImage markerImage = MapImageFactory.fromResource(getResources(), R.drawable.ic_current_location);
            MapMarker marker = new MapMarker(coordinates, markerImage);
            searchMarkers.add(marker);
            mapView.getMapScene().addMapMarker(marker);
        } catch (Exception e) {
            Log.e(TAG, "Error updating map location: " + e.getMessage());
        }
    }
    
    private void clearSearchMarkers() {
        if (mapView == null || !isMapViewInitialized) return;
        
        try {
            for (MapMarker marker : searchMarkers) {
                mapView.getMapScene().removeMapMarker(marker);
            }
            searchMarkers.clear();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing search markers: " + e.getMessage());
        }
    }
    
    private void clearPolylines() {
        if (mapView == null || !isMapViewInitialized) return;
        
        try {
            for (MapPolyline polyline : mapPolylines) {
                mapView.getMapScene().removeMapPolyline(polyline);
            }
            mapPolylines.clear();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing polylines: " + e.getMessage());
        }
    }
    
    private void performSearch(String query) {
        if (currentLocation == null) {
            showToast("Current location not available");
            return;
        }
        
        // Execute search in background thread
        new Thread(() -> {
            try {
                // Clear existing markers and results
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        clearSearchMarkers();
                        clearPolylines();
                        searchResultsTitles.clear();
                        searchResultsCoordinates.clear();
                    });
                }
                
                // Perform search request
                double lat = currentLocation.latitude;
                double lng = currentLocation.longitude;
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String urlString = String.format(
                        java.util.Locale.US,
                        "https://discover.search.hereapi.com/v1/discover?apikey=%s&q=%s&at=%f,%f&limit=5",
                        SEARCH_API_KEY, encodedQuery, lat, lng
                );
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parse results
                    parseSearchResults(response.toString());
                } else {
                    showToast("Search failed: " + responseCode);
                }
                
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Exception during search: " + e.getMessage());
                showToast("Search failed: " + e.getMessage());
            }
        }).start();
    }
    
    private void parseSearchResults(String jsonResponse) {
        if (getActivity() == null) return;
        
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray items = jsonObject.getJSONArray("items");
            
            final List<String> titles = new ArrayList<>();
            final List<GeoCoordinates> coordinates = new ArrayList<>();
            
            int count = Math.min(items.length(), 5);
            for (int i = 0; i < count; i++) {
                JSONObject item = items.getJSONObject(i);
                JSONObject position = item.getJSONObject("position");
                double lat = position.getDouble("lat");
                double lng = position.getDouble("lng");
                String title = item.getString("title");
                
                titles.add(title);
                coordinates.add(new GeoCoordinates(lat, lng));
            }
            
            getActivity().runOnUiThread(() -> {
                if (!isFragmentActive) return;
                
                searchResultsTitles.clear();
                searchResultsCoordinates.clear();
                searchResultsTitles.addAll(titles);
                searchResultsCoordinates.addAll(coordinates);
                
                if (searchResultsTitles.isEmpty()) {
                    showToast("No results found");
                    return;
                }
                
                // Update UI
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_list_item_1, searchResultsTitles);
                searchResultsList.setAdapter(adapter);
                searchResultsList.setVisibility(View.VISIBLE);
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing search results: " + e.getMessage());
            showToast("Error parsing search results");
        }
    }
    
    private void calculateRoute(GeoCoordinates start, GeoCoordinates destination) {
        if (routingEngine == null || start == null || destination == null || 
                mapView == null || !isMapViewInitialized) {
            showToast("Unable to calculate route");
            return;
        }
        
        try {
            // Show loading message
            showToast("Calculating route...");
            
            List<Waypoint> waypoints = new ArrayList<>();
            waypoints.add(new Waypoint(start));
            waypoints.add(new Waypoint(destination));
            
            // Set a timeout
            Handler timeoutHandler = new Handler(Looper.getMainLooper());
            Runnable timeoutRunnable = () -> showToast("Route calculation timed out");
            timeoutHandler.postDelayed(timeoutRunnable, 30000);
            
            // Calculate route
            routingEngine.calculateRoute(waypoints, getCarOptions(), (routingError, routes) -> {
                // Cancel timeout
                timeoutHandler.removeCallbacks(timeoutRunnable);
                
                if (!isFragmentActive || getActivity() == null || mapView == null) return;
                
                if (routingError == null && routes != null && !routes.isEmpty()) {
                    Route route = routes.get(0);
                    
                    getActivity().runOnUiThread(() -> {
                        try {
                            showRouteOnMap(route);
                            displayRouteInfo(route);
                            
                            // Only fetch potholes if the route displays correctly
                            // to avoid unnecessary processing
                            fetchPotholesOnRoute(route);
                        } catch (Exception e) {
                            Log.e(TAG, "Error displaying route: " + e.getMessage());
                        }
                    });
                } else {
                    showToast("No route found");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error calculating route: " + e.getMessage());
            showToast("Error calculating route");
        }
    }
    
    private CarOptions getCarOptions() {
        CarOptions carOptions = new CarOptions();
        carOptions.routeOptions.enableTolls = true;
        carOptions.routeOptions.trafficOptimizationMode = trafficDisabled ?
                TrafficOptimizationMode.DISABLED : TrafficOptimizationMode.TIME_DEPENDENT;
        return carOptions;
    }
    
    private void showRouteOnMap(Route route) {
        if (mapView == null || !isMapViewInitialized) return;
        
        try {
            // Clear existing routes
            clearPolylines();
            
            // Create polyline
            GeoPolyline routeGeoPolyline = route.getGeometry();
            float widthInPixels = 20;
            Color polylineColor = new Color(0, (float)0.56, (float)0.54, (float)0.63);
            
            // Create polyline with error handling
            try {
                MapPolyline routeMapPolyline = new MapPolyline(
                        routeGeoPolyline,
                        new MapPolyline.SolidRepresentation(
                                new MapMeasureDependentRenderSize(RenderSize.Unit.PIXELS, widthInPixels),
                                polylineColor,
                                LineCap.ROUND
                        )
                );
                
                // Add to map and track
                mapView.getMapScene().addMapPolyline(routeMapPolyline);
                mapPolylines.add(routeMapPolyline);
                
                // Add markers for start and destination
                List<GeoCoordinates> vertices = routeGeoPolyline.vertices;
                if (!vertices.isEmpty()) {
                    addSearchMarker(vertices.get(0));
                    addSearchMarker(vertices.get(vertices.size() - 1));
                }
                
                // Zoom to show the entire route
                zoomToRoute(vertices);
                
            } catch (Exception e) {
                Log.e(TAG, "Error creating route polyline: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing route on map: " + e.getMessage());
        }
    }
    
    private void zoomToRoute(List<GeoCoordinates> routePoints) {
        if (mapView == null || routePoints.size() < 2) return;
        
        try {
            GeoCoordinates start = routePoints.get(0);
            GeoCoordinates end = routePoints.get(routePoints.size() - 1);
            
            // Calculate midpoint
            GeoCoordinates center = new GeoCoordinates(
                    (start.latitude + end.latitude) / 2,
                    (start.longitude + end.longitude) / 2
            );
            
            // Calculate distance between points
            double distance = calculateDistance(start, end);
            double zoomDistance = Math.max(distance * 1.2, 1000);
            
            // Apply zoom
            MapMeasure mapMeasure = new MapMeasure(MapMeasure.Kind.DISTANCE, zoomDistance);
            mapView.getCamera().lookAt(center, mapMeasure);
        } catch (Exception e) {
            Log.e(TAG, "Error zooming to route: " + e.getMessage());
        }
    }
    
    private double calculateDistance(GeoCoordinates point1, GeoCoordinates point2) {
        final int R = 6371; // Earth radius in kilometers
        
        double lat1 = Math.toRadians(point1.latitude);
        double lon1 = Math.toRadians(point1.longitude);
        double lat2 = Math.toRadians(point2.latitude);
        double lon2 = Math.toRadians(point2.longitude);
        
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return R * c * 1000; // Convert to meters
    }
    
    private void displayRouteInfo(Route route) {
        if (estimatedTimeTextView == null) return;
        
        try {
            // Format duration
            long durationInSeconds = route.getDuration().getSeconds();
            String timeText;
            
            if (durationInSeconds < 60) {
                timeText = durationInSeconds + " seconds";
            } else if (durationInSeconds < 3600) {
                timeText = (durationInSeconds / 60) + " minutes";
            } else {
                long hours = durationInSeconds / 3600;
                long minutes = (durationInSeconds % 3600) / 60;
                timeText = hours + "h " + minutes + "m";
            }
            
            // Format distance
            int lengthInMeters = route.getLengthInMeters();
            String distanceText;
            
            if (lengthInMeters < 1000) {
                distanceText = lengthInMeters + " m";
            } else {
                distanceText = String.format("%.1f km", lengthInMeters / 1000.0);
            }
            
            // Display info
            String routeInfo = "Distance: " + distanceText + " • Time: " + timeText;
            estimatedTimeTextView.setText(routeInfo);
            estimatedTimeTextView.setVisibility(View.VISIBLE);
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying route info: " + e.getMessage());
        }
    }
    
    private void addSearchMarker(GeoCoordinates coordinates) {
        if (mapView == null || !isMapViewInitialized) return;
        
        try {
            MapImage markerImage = MapImageFactory.fromResource(getResources(), R.drawable.ic_current_location);
            MapMarker marker = new MapMarker(coordinates, markerImage);
            searchMarkers.add(marker);
            mapView.getMapScene().addMapMarker(marker);
        } catch (Exception e) {
            Log.e(TAG, "Error adding search marker: " + e.getMessage());
        }
    }
    
    private void fetchPotholesOnRoute(Route route) {
        if (!isFragmentActive || potholesOnRoute == null) return;
        
        // Clear existing potholes
        potholesOnRoute.clear();
        
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("potholes");
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isFragmentActive || getActivity() == null) return;
                
                List<GeoCoordinates> routeCoordinates = route.getGeometry().vertices;
                List<Pothole> nearbyPotholes = new ArrayList<>();
                
                for (DataSnapshot data : snapshot.getChildren()) {
                    Pothole pothole = data.getValue(Pothole.class);
                    if (pothole != null) {
                        GeoCoordinates potholeCoordinates = new GeoCoordinates(
                                pothole.getLatitude(), pothole.getLongitude());
                        
                        // Check if pothole is on route
                        if (isPotholeNearRoute(routeCoordinates, potholeCoordinates, 50)) {
                            nearbyPotholes.add(pothole);
                            
                            // Add marker on UI thread
                            getActivity().runOnUiThread(() -> {
                                if (isFragmentActive && mapView != null) {
                                    addPotholeMarker(potholeCoordinates, pothole.getLevel());
                                }
                            });
                        }
                    }
                }
                
                // Update pothole list and start monitoring if needed
                potholesOnRoute.addAll(nearbyPotholes);
                if (!potholesOnRoute.isEmpty()) {
                    startPotholeMonitoring();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching pothole data: " + error.getMessage());
            }
        });
    }
    
    private boolean isPotholeNearRoute(List<GeoCoordinates> routeCoordinates, 
                                      GeoCoordinates potholeCoordinates, 
                                      double thresholdMeters) {
        // Quick check if route is empty
        if (routeCoordinates.isEmpty()) return false;
        
        // Check each segment
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            double distance = distanceToSegment(
                    potholeCoordinates,
                    routeCoordinates.get(i),
                    routeCoordinates.get(i+1)
            );
            
            if (distance <= thresholdMeters) {
                return true;
            }
        }
        
        return false;
    }
    
    private double distanceToSegment(GeoCoordinates p, GeoCoordinates v, GeoCoordinates w) {
        // Calculate distance from point p to line segment [v,w]
        double l2 = Math.pow(v.latitude - w.latitude, 2) + Math.pow(v.longitude - w.longitude, 2);
        
        // If segment is a point, return distance to the point
        if (l2 == 0) return calculateDistance(p, v);
        
        // Project point onto line
        double t = ((p.latitude - v.latitude) * (w.latitude - v.latitude) + 
                   (p.longitude - v.longitude) * (w.longitude - v.longitude)) / l2;
        
        if (t < 0) return calculateDistance(p, v); // Beyond v
        if (t > 1) return calculateDistance(p, w); // Beyond w
        
        // Projection falls on segment
        GeoCoordinates projection = new GeoCoordinates(
                v.latitude + t * (w.latitude - v.latitude),
                v.longitude + t * (w.longitude - v.longitude)
        );
        
        return calculateDistance(p, projection);
    }
    
    private void addPotholeMarker(GeoCoordinates coordinates, String level) {
        if (mapView == null || !isMapViewInitialized) return;
        
        try {
            MapImage markerImage = MapImageFactory.fromResource(getResources(), R.drawable.ic_warning);
            MapMarker marker = new MapMarker(coordinates, markerImage);
            searchMarkers.add(marker); // Add to same list for cleanup
            mapView.getMapScene().addMapMarker(marker);
        } catch (Exception e) {
            Log.e(TAG, "Error adding pothole marker: " + e.getMessage());
        }
    }
    
    private void startPotholeMonitoring() {
        if (!isFragmentActive || potholesOnRoute.isEmpty()) return;
        
        // Stop existing monitoring
        if (notificationHandler != null && notificationRunnable != null) {
            notificationHandler.removeCallbacks(notificationRunnable);
        }
        
        // Create new handler if needed
        if (notificationHandler == null) {
            notificationHandler = new Handler(Looper.getMainLooper());
        }
        
        final long[] lastNotificationTime = {0};
        
        notificationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFragmentActive || getContext() == null) return;
                
                try {
                    if (currentLocation != null && !potholesOnRoute.isEmpty()) {
                        // Check for nearby potholes
                        checkForNearbyPotholes(lastNotificationTime);
                    }
                    
                    // Continue monitoring if still active
                    if (isFragmentActive) {
                        notificationHandler.postDelayed(this, 5000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error monitoring potholes: " + e.getMessage());
                }
            }
        };
        
        // Start monitoring
        notificationHandler.post(notificationRunnable);
    }
    
    private void checkForNearbyPotholes(long[] lastNotificationTime) {
        long currentTime = System.currentTimeMillis();
        
        // Limit notification frequency
        if (currentTime - lastNotificationTime[0] < 10000) return;
        
        for (Pothole pothole : potholesOnRoute) {
            GeoCoordinates potholeCoordinates = new GeoCoordinates(
                    pothole.getLatitude(), pothole.getLongitude());
            
            double distance = calculateDistance(currentLocation, potholeCoordinates);
            
            if (distance <= 200) {
                // Show notification
                showPotholeNotification(pothole, (int)distance);
                lastNotificationTime[0] = currentTime;
                break; // Show only one notification at a time
            }
        }
    }
    
    private void showPotholeNotification(Pothole pothole, int distance) {
        if (!isFragmentActive || getActivity() == null) return;
        
        try {
            NotificationManager notificationManager = 
                    (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Create channel for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        "pothole_channel",
                        "Pothole Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationManager.createNotificationChannel(channel);
            }
            
            // Build notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), "pothole_channel")
                    .setSmallIcon(R.drawable.ic_warning)
                    .setContentTitle("Pothole Alert")
                    .setContentText("Approaching a " + pothole.getLevel() + " pothole! Distance: " + distance + "m")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            
            // Show notification
            notificationManager.notify(1, builder.build());
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage());
        }
    }
    
    private void showToast(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        // Stop location updates
        stopLocationUpdates();
        
        // Stop pothole monitoring
        if (notificationHandler != null && notificationRunnable != null) {
            notificationHandler.removeCallbacks(notificationRunnable);
        }
        
        // Pause map view
        if (mapView != null) {
            mapView.onPause();
        }
        
        isFragmentActive = false;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Cancel handlers
        if (notificationHandler != null && notificationRunnable != null) {
            notificationHandler.removeCallbacks(notificationRunnable);
            notificationRunnable = null;
        }
        
        // Clear resources
        if (mapView != null && isMapViewInitialized) {
            try {
                clearSearchMarkers();
                clearPolylines();
                
                if (currentLocationMarker != null) {
                    mapView.getMapScene().removeMapMarker(currentLocationMarker);
                    currentLocationMarker = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error clearing map resources: " + e.getMessage());
            }
        }
        
        // Stop location updates
        stopLocationUpdates();
        
        // Reset state
        potholesOnRoute.clear();
        isFragmentActive = false;
        isMapViewInflated = false;
        isMapViewInitialized = false;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Destroy map view
        if (mapView != null) {
            try {
                mapView.onDestroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destroying map view: " + e.getMessage());
            } finally {
                mapView = null;
            }
        }
        
        // Clear references
        searchMarkers.clear();
        searchResultsCoordinates.clear();
        searchResultsTitles.clear();
        mapPolylines.clear();
        currentLocation = null;
        previousLocation = null;
        
        // Don't destroy SDK engine here, as it's shared across the app
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        
        if (mapView != null && isMapViewInitialized) {
            try {
                mapView.onSaveInstanceState(outState);
            } catch (Exception e) {
                Log.e(TAG, "Error saving map state: " + e.getMessage());
            }
        }
    }
}