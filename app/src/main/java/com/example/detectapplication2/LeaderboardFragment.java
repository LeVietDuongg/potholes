package com.example.detectapplication2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LeaderboardFragment extends Fragment {

    // UI Components
    private TextView textViewLeaderboardTitle;
    private TextView textViewDateRange;
    private RecyclerView recyclerViewLeaderboard;
    private TextView textViewFirstPlaceName;
    private TextView textViewFirstPlaceCount;
    private TextView textViewSecondPlaceName;
    private TextView textViewSecondPlaceCount;
    private TextView textViewThirdPlaceName;
    private TextView textViewThirdPlaceCount;
    private TextView textViewCurrentUserRank;
    private TextView textViewCurrentUserName;
    private TextView textViewCurrentUserCount;
    private ImageButton buttonPreviousWeek;
    private ImageButton buttonNextWeek;

    // Data
    private List<LeaderboardUser> userList;
    private LeaderboardAdapter adapter;
    private String currentUserId;
    private int currentWeek = 0; // 0 là tuần hiện tại, -1 là tuần trước, -2 là tuần trước nữa...
    private Map<String, Integer> userPotholeCounts; // <userId, số lượng ổ gà>

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        
        // Khởi tạo UI components
        initViews(view);
        
        // Khởi tạo data
        initData();
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set up listeners
        setupListeners();
        
        // Load dữ liệu cho tuần hiện tại
        loadLeaderboardData();
        
        return view;
    }

    private void initViews(View view) {
        textViewLeaderboardTitle = view.findViewById(R.id.textViewLeaderboardTitle);
        textViewDateRange = view.findViewById(R.id.textViewDateRange);
        recyclerViewLeaderboard = view.findViewById(R.id.recyclerViewLeaderboard);
        textViewFirstPlaceName = view.findViewById(R.id.textViewFirstPlaceName);
        textViewFirstPlaceCount = view.findViewById(R.id.textViewFirstPlaceCount);
        textViewSecondPlaceName = view.findViewById(R.id.textViewSecondPlaceName);
        textViewSecondPlaceCount = view.findViewById(R.id.textViewSecondPlaceCount);
        textViewThirdPlaceName = view.findViewById(R.id.textViewThirdPlaceName);
        textViewThirdPlaceCount = view.findViewById(R.id.textViewThirdPlaceCount);
        textViewCurrentUserRank = view.findViewById(R.id.textViewCurrentUserRank);
        textViewCurrentUserName = view.findViewById(R.id.textViewCurrentUserName);
        textViewCurrentUserCount = view.findViewById(R.id.textViewCurrentUserCount);
        buttonPreviousWeek = view.findViewById(R.id.buttonPreviousWeek);
        buttonNextWeek = view.findViewById(R.id.buttonNextWeek);
    }

    private void initData() {
        userList = new ArrayList<>();
        userPotholeCounts = new HashMap<>();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter(getContext(), userList);
        recyclerViewLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewLeaderboard.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonPreviousWeek.setOnClickListener(v -> {
            currentWeek--;
            loadLeaderboardData();
        });
        
        buttonNextWeek.setOnClickListener(v -> {
            if (currentWeek < 0) {
                currentWeek++;
                loadLeaderboardData();
            } else {
                Toast.makeText(getContext(), "Đây là tuần hiện tại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLeaderboardData() {
        // Update UI trước khi tải dữ liệu
        updateWeekInfo();
        
        // Reset dữ liệu
        userList.clear();
        userPotholeCounts.clear();
        
        // Tải dữ liệu từ Firebase
        loadUserData();
    }

    private void updateWeekInfo() {
        // Cập nhật tiêu đề và phạm vi ngày trong tuần
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, currentWeek);
        
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        
        // Set về ngày đầu của tuần (Thứ 2)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String startDate = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(calendar.getTime());
        
        // Set về ngày cuối của tuần (Chủ nhật)
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        String endDate = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(calendar.getTime());
        
        textViewLeaderboardTitle.setText(String.format("Bảng xếp hạng tuần %d - %d", weekOfYear, year));
        textViewDateRange.setText(String.format("%s - %s/%d", startDate, endDate, year));
        
        // Disable nút Next nếu đang ở tuần hiện tại
        buttonNextWeek.setEnabled(currentWeek < 0);
        buttonNextWeek.setAlpha(currentWeek < 0 ? 1.0f : 0.5f);
    }

    private void loadUserData() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Tạo mapping User ID -> Tên người dùng
                Map<String, String> userNames = new HashMap<>();
                
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String name = userSnapshot.child("name").getValue(String.class);
                    
                    if (name != null) {
                        userNames.put(userId, name);
                    }
                }
                
                // Tiếp tục tải dữ liệu ổ gà
                loadPotholeData(userNames);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPotholeData(Map<String, String> userNames) {
        DatabaseReference potholeRef = FirebaseDatabase.getInstance().getReference("potholes");
        
        // Lấy thời gian bắt đầu và kết thúc của tuần
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, currentWeek);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startOfWeek = calendar.getTimeInMillis();
        
        calendar.add(Calendar.DAY_OF_WEEK, 7);
        long endOfWeek = calendar.getTimeInMillis();
        
        potholeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Reset danh sách trước khi thêm mới
                userPotholeCounts.clear();
                
                // Đếm số ổ gà của mỗi người dùng trong tuần được chọn
                for (DataSnapshot potholeSnapshot : snapshot.getChildren()) {
                    Pothole pothole = potholeSnapshot.getValue(Pothole.class);
                    
                    if (pothole != null && pothole.getUserId() != null) {
                        // Trong bài tập này, do không có thông tin timestamp,
                        // chúng ta sẽ giả định tất cả ổ gà đều trong tuần hiện tại của người dùng
                        // Trong thực tế, bạn sẽ cần thêm trường timestamp vào lớp Pothole
                        // và kiểm tra xem pothole.getTimestamp() có nằm trong khoảng startOfWeek và endOfWeek không
                        
                        String userId = pothole.getUserId();
                        int count = userPotholeCounts.getOrDefault(userId, 0);
                        userPotholeCounts.put(userId, count + 1);
                    }
                }
                
                // Tạo danh sách người dùng
                createLeaderboard(userNames);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createLeaderboard(Map<String, String> userNames) {
        userList.clear();
        
        // Thêm tất cả người dùng có ổ gà vào danh sách
        for (Map.Entry<String, Integer> entry : userPotholeCounts.entrySet()) {
            String userId = entry.getKey();
            int count = entry.getValue();
            String username = userNames.getOrDefault(userId, "Người dùng không xác định");
            
            LeaderboardUser user = new LeaderboardUser(userId, username, count);
            
            // Đánh dấu nếu là người dùng hiện tại
            if (userId.equals(currentUserId)) {
                user.setCurrentUser(true);
            }
            
            userList.add(user);
        }
        
        // Sắp xếp theo số lượng ổ gà (giảm dần)
        Collections.sort(userList, new Comparator<LeaderboardUser>() {
            @Override
            public int compare(LeaderboardUser u1, LeaderboardUser u2) {
                return Integer.compare(u2.getPotholeCount(), u1.getPotholeCount());
            }
        });
        
        // Gán thứ hạng
        for (int i = 0; i < userList.size(); i++) {
            userList.get(i).setRank(i + 1);
        }
        
        // Cập nhật adapter
        adapter.updateData(userList);
        
        // Hiển thị top 3 người dùng
        displayTop3Users();
        
        // Hiển thị thông tin người dùng hiện tại
        displayCurrentUserInfo();
    }

    private void displayTop3Users() {
        // Ẩn tất cả thông tin top 3 trước
        textViewFirstPlaceName.setText("");
        textViewFirstPlaceCount.setText("");
        textViewSecondPlaceName.setText("");
        textViewSecondPlaceCount.setText("");
        textViewThirdPlaceName.setText("");
        textViewThirdPlaceCount.setText("");
        
        // Hiển thị các người dùng trong top 3 (nếu có)
        if (userList.size() > 0) {
            textViewFirstPlaceName.setText(userList.get(0).getUsername());
            textViewFirstPlaceCount.setText(String.valueOf(userList.get(0).getPotholeCount()));
        }
        
        if (userList.size() > 1) {
            textViewSecondPlaceName.setText(userList.get(1).getUsername());
            textViewSecondPlaceCount.setText(String.valueOf(userList.get(1).getPotholeCount()));
        }
        
        if (userList.size() > 2) {
            textViewThirdPlaceName.setText(userList.get(2).getUsername());
            textViewThirdPlaceCount.setText(String.valueOf(userList.get(2).getPotholeCount()));
        }
    }

    private void displayCurrentUserInfo() {
        // Tìm người dùng hiện tại trong danh sách
        LeaderboardUser currentUser = null;
        for (LeaderboardUser user : userList) {
            if (user.getUserId().equals(currentUserId)) {
                currentUser = user;
                break;
            }
        }
        
        if (currentUser != null) {
            // Hiển thị thông tin người dùng hiện tại
            textViewCurrentUserRank.setText(String.valueOf(currentUser.getRank()));
            textViewCurrentUserName.setText("Bạn");
            textViewCurrentUserCount.setText(String.valueOf(currentUser.getPotholeCount()));
        } else {
            // Nếu người dùng chưa đóng góp ổ gà nào
            textViewCurrentUserRank.setText("-");
            textViewCurrentUserName.setText("Bạn");
            textViewCurrentUserCount.setText("0");
        }
    }
}