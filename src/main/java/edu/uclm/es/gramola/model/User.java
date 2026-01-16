package edu.uclm.es.gramola.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user")
public class User {

    @Id
    @Column(length = 255)
    private String email;

    @Column(nullable = false)
    private String bar;

    @Column(nullable = false)
    private String pwd;

    @Column(name = "client_id")
    private String spotifyClientId;

    @Column(name = "client_secret")
    private String spotifyClientSecret;
    
    // Para confirmar el email
    @Column(name = "creation_token_id")
    private String creationTokenId;

    // Para saber si ha confirmado el correo
    private boolean enabled; 

    // Para saber si ha pagado
   @Column(name = "subscription_active", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean subscriptionActive = false;

    // Constructores, Getters y Setters
    public User() {
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBar() { return bar; }
    public void setBar(String bar) { this.bar = bar; }

    public String getPwd() { return pwd; }
    public void setPwd(String pwd) { this.pwd = pwd; }

    public String getSpotifyClientId() { return spotifyClientId; }
    public void setSpotifyClientId(String spotifyClientId) { this.spotifyClientId = spotifyClientId; }

    public String getSpotifyClientSecret() { return spotifyClientSecret; }
    public void setSpotifyClientSecret(String spotifyClientSecret) { this.spotifyClientSecret = spotifyClientSecret; }

    public String getCreationTokenId() { return creationTokenId; }
    public void setCreationTokenId(String creationTokenId) { this.creationTokenId = creationTokenId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isSubscriptionActive() { return subscriptionActive; }
    public void setSubscriptionActive(boolean subscriptionActive) { this.subscriptionActive = subscriptionActive; }
}