package com.jtune.model;

public class Song {
    private int id;
    private String title;
    private String artist;
    private String filePath;
    private String addedAt;

    public Song() {
    }

    public Song(int id, String title, String artist, String filePath, String addedAt) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.filePath = filePath;
        this.addedAt = addedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public String toString() {
        return title + " - " + artist;
    }
}
