package edu.uclm.es.gramola.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "token")
public class Token {
    @Id
    private String id;
    private long creationTime;
    private Long useTime;

    public Token() {}

    public Token(String id) {
        this.id = id;
        this.creationTime = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }
    public Long getUseTime() { return useTime; }
    public void setUseTime(Long useTime) { this.useTime = useTime; }
}