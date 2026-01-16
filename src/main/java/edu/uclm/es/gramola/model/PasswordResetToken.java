package edu.uclm.es.gramola.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private long creationTime;

    @Column(nullable = false)
    private long expiresAt;

    @Column
    private Long useTime;

    public PasswordResetToken() {
    }

    public PasswordResetToken(String id, String email, long expiresAt) {
        this.id = id;
        this.email = email;
        this.creationTime = System.currentTimeMillis();
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getUseTime() {
        return useTime;
    }

    public void setUseTime(Long useTime) {
        this.useTime = useTime;
    }
}
