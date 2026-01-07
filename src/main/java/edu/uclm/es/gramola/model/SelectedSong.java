package edu.uclm.es.gramola.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class SelectedSong {
    @Id
    private String id;
    
    private String title;
    private String artist;
    private String spotifyId;
    private long time; // Para saber cuándo se pidió

    @ManyToOne
    private User bar; // El bar donde sonó

    public SelectedSong() {
        this.id = UUID.randomUUID().toString();
        this.time = System.currentTimeMillis();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getSpotifyId() { return spotifyId; }
    public void setSpotifyId(String spotifyId) { this.spotifyId = spotifyId; }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }

    public User getBar() { return bar; }
    public void setBar(User bar) { this.bar = bar; }
}