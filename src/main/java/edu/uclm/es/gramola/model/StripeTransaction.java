package edu.uclm.es.gramola.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class StripeTransaction {
    @Id
    private String id;
    
    private String email;
    
    @Lob // Usamos Lob para guardar el JSON como texto largo
    private String data; 
    
    private long amount; // Importe en céntimos

    public StripeTransaction() {
        this.id = UUID.randomUUID().toString();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
}