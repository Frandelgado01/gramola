package edu.uclm.es.gramola.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pricing") // Esto conecta con la tabla de tu foto
public class Pricing {
    
    @Id
    private String type; // 'monthly', 'annual', 'song'
    
    private Double price;
    
    private String currency;

    public Pricing() {}

    // Getters y Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    // Método auxiliar para pasar euros a céntimos (Stripe lo necesita así)
    public long getPriceInCents() {
        if (this.price == null) return 0;
        return (long) (this.price * 100);
    }
}