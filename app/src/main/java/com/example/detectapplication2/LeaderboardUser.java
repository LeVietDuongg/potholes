package com.example.detectapplication2;

/**
 * Lớp mô hình biểu diễn thông tin người dùng trong bảng xếp hạng
 */
public class LeaderboardUser {
    private String userId;
    private String username;
    private int potholeCount;
    private int rank;
    private boolean isCurrentUser;

    public LeaderboardUser() {
        // Constructor mặc định cho Firebase
    }

    public LeaderboardUser(String userId, String username, int potholeCount) {
        this.userId = userId;
        this.username = username;
        this.potholeCount = potholeCount;
        this.rank = 0;
        this.isCurrentUser = false;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getPotholeCount() {
        return potholeCount;
    }

    public void setPotholeCount(int potholeCount) {
        this.potholeCount = potholeCount;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public boolean isCurrentUser() {
        return isCurrentUser;
    }

    public void setCurrentUser(boolean currentUser) {
        isCurrentUser = currentUser;
    }
}