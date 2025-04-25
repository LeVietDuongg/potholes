package com.example.detectapplication2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private List<LeaderboardUser> userList;
    private Context context;

    public LeaderboardAdapter(Context context, List<LeaderboardUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        LeaderboardUser user = userList.get(position);
        
        // Set thứ hạng
        holder.textViewRank.setText(String.valueOf(user.getRank()));
        
        // Set tên người dùng
        holder.textViewUsername.setText(user.getUsername());
        
        // Set số lượng ổ gà đã đóng góp
        holder.textViewPotholeCount.setText(String.valueOf(user.getPotholeCount()));
        
        // Đổi màu nếu là người dùng hiện tại
        if (user.isCurrentUser()) {
            holder.textViewRank.setTextColor(ContextCompat.getColor(context, R.color.purple_500));
            holder.textViewUsername.setTextColor(ContextCompat.getColor(context, R.color.purple_500));
            holder.textViewPotholeCount.setTextColor(ContextCompat.getColor(context, R.color.purple_500));
        } else {
            holder.textViewRank.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.textViewUsername.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.textViewPotholeCount.setTextColor(ContextCompat.getColor(context, R.color.black));
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateData(List<LeaderboardUser> newUserList) {
        this.userList = newUserList;
        notifyDataSetChanged();
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRank;
        TextView textViewUsername;
        TextView textViewPotholeCount;

        LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRank = itemView.findViewById(R.id.textViewRank);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewPotholeCount = itemView.findViewById(R.id.textViewPotholeCount);
        }
    }
}