package edu.uclm.es.gramola.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.model.StripeTransaction;
import edu.uclm.es.gramola.services.PaymentService;

@RestController
@RequestMapping("/gramola/payments") // Ojo a la ruta
@CrossOrigin(origins = "http://127.0.0.1:4200")
public class PaymentsController {

    @Autowired
    private PaymentService service;

    @PostMapping("/prepay")
    public StripeTransaction prepay(@RequestBody Map<String, Object> info) {
        try {
            // Asumimos que viene el email y la cantidad
            String email = (String) info.get("email");
            // Cobraremos 10.00 euros (1000 céntimos) por defecto si no viene nada
            long amount = 1000L; 
            
            return this.service.prepay(amount, email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}