package com.example.ieperuanosuizoapp;

import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

public class LeaderboardAdapter extends ListAdapter<Player, LeaderboardAdapter.ViewHolder> {

    private final Map<String, Integer> scoreHistory = new HashMap<>();
    private String metricSuffix = " d\u00edas";

    public LeaderboardAdapter() {
        super(new DiffUtil.ItemCallback<Player>() {
            @Override
            public boolean areItemsTheSame(@NonNull Player oldItem, @NonNull Player newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Player oldItem, @NonNull Player newItem) {
                return oldItem.equals(newItem);
            }
        });
    }

    public void setMetricSuffix(String suffix) {
        this.metricSuffix = suffix;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Player player = getItem(position);
        holder.bind(player, player.getRank(), scoreHistory, metricSuffix);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRank;
        private final TextView tvAvatarText;
        private final TextView tvName;
        private final TextView tvLevelBadge;
        private final TextView tvTitle;
        private final TextView tvScore;
        private final TextView tvScoreSuffix;
        private final TextView tvTrendArrow;
        private final ImageView ivTrendIcon;
        private final View layoutTrendCapsule;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvAvatarText = itemView.findViewById(R.id.tv_avatar_text);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLevelBadge = itemView.findViewById(R.id.tv_level_badge);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvScore = itemView.findViewById(R.id.tv_score);
            tvScoreSuffix = itemView.findViewById(R.id.tv_score_suffix);
            tvTrendArrow = itemView.findViewById(R.id.tv_trend_arrow);
            ivTrendIcon = itemView.findViewById(R.id.iv_trend_icon);
            layoutTrendCapsule = itemView.findViewById(R.id.layout_trend_capsule);
        }

        void bind(Player player, int rankValue, Map<String, Integer> scoreHistory, String suffix) {
            tvRank.setText(String.valueOf(rankValue));
            tvAvatarText.setText(player.getAvatarInitials());
            tvAvatarText.setVisibility(View.VISIBLE);
            tvName.setText(player.getName());
            if (tvLevelBadge != null) tvLevelBadge.setVisibility(View.GONE);
            if (tvTitle != null) {
                String t = player.getTitle();
                tvTitle.setText(t != null && !t.isEmpty() ? t : "");
            }
            if (tvScoreSuffix != null) tvScoreSuffix.setText(suffix);

            int currentScore = player.getScore();
            Integer prev = scoreHistory.get(player.getId());
            int previousScore = prev != null ? prev : currentScore;
            scoreHistory.put(player.getId(), currentScore);

            if (previousScore != currentScore) {
                ValueAnimator animator = ValueAnimator.ofInt(previousScore, currentScore);
                animator.setDuration(500);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(animation -> tvScore.setText(animation.getAnimatedValue().toString()));
                animator.start();
            } else {
                tvScore.setText(String.valueOf(currentScore));
            }

            tvTrendArrow.setText(player.getTrendText());
            if (player.getTrendType() > 0) {
                if (ivTrendIcon != null) ivTrendIcon.setImageResource(R.drawable.ic_trend_up);
                if (layoutTrendCapsule != null) layoutTrendCapsule.setBackgroundResource(R.drawable.bg_trend_up_capsule);
                tvTrendArrow.setTextColor(itemView.getContext().getColor(R.color.emerald));
            } else if (player.getTrendType() < 0) {
                if (ivTrendIcon != null) ivTrendIcon.setImageResource(R.drawable.ic_trend_down);
                if (layoutTrendCapsule != null) layoutTrendCapsule.setBackgroundResource(R.drawable.bg_trend_down_capsule);
                tvTrendArrow.setTextColor(itemView.getContext().getColor(R.color.neon_pink));
            } else {
                if (ivTrendIcon != null) ivTrendIcon.setImageResource(R.drawable.ic_stable_gray);
                if (layoutTrendCapsule != null) layoutTrendCapsule.setBackgroundResource(R.drawable.bg_trend_stable_capsule);
                tvTrendArrow.setTextColor(itemView.getContext().getColor(R.color.text_gray));
            }
        }
    }
}
