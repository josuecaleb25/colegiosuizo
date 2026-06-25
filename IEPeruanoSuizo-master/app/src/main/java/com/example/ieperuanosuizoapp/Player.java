package com.example.ieperuanosuizoapp;

import java.util.Objects;

public class Player {
    private final String id;
    private final String name;
    private int score;
    private final int level;
    private final String title;
    private final String avatarInitials;
    private String trendText;
    private int trendType;
    private final String aulaId;
    private int rank;

    public Player(String id, String name, int score, int level, String title, String aulaId) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.level = level;
        this.title = title;
        this.aulaId = aulaId;
        this.avatarInitials = calculateInitials(name);
        this.trendText = "\u2014";
        this.trendType = 0;
        this.rank = 0;
    }

    public String getAulaId() { return aulaId; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getId() { return id; }
    public String getName() { return name; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public String getAvatarInitials() { return avatarInitials; }
    public String getTrendText() { return trendText; }
    public void setTrendText(String trendText) { this.trendText = trendText; }
    public int getTrendType() { return trendType; }
    public void setTrendType(int trendType) { this.trendType = trendType; }

    private String calculateInitials(String inputName) {
        if (inputName == null || inputName.trim().isEmpty()) return "??";
        String[] parts = inputName.trim().split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty())
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        if (parts[0].length() >= 2) return parts[0].substring(0, 2).toUpperCase();
        return parts[0].toUpperCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return score == player.score && level == player.level && trendType == player.trendType && rank == player.rank && Objects.equals(id, player.id) && Objects.equals(name, player.name) && Objects.equals(title, player.title) && Objects.equals(trendText, player.trendText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, score, level, title, trendText, trendType, rank);
    }

    public Player copy() {
        Player copy = new Player(this.id, this.name, this.score, this.level, this.title, this.aulaId);
        copy.setTrendText(this.trendText);
        copy.setTrendType(this.trendType);
        copy.setRank(this.rank);
        return copy;
    }
}
