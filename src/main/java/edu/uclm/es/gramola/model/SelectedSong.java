package edu.uclm.es.gramola.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class SelectedSong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    private String title;
    private String artist;
    private String spotifyId;
    
    // Opcional: Relación con el Bar (User)
    // De momento lo dejamos simple para que no falle
    // @ManyToOne 
    // private User bar; 

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    
    public String getSpotifyId() { return spotifyId; }
    public void setSpotifyId(String spotifyId) { this.spotifyId = spotifyId; }
}