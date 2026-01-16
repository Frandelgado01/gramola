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
import edu.uclm.es.gramola.services.UserService;

@RestController
@RequestMapping("/gramola/payments")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class PaymentsController {

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private UserService userService; // Necesario para activar la suscripción después de pagar

    @PostMapping("/prepay")
    public StripeTransaction prepay(@RequestBody Map<String, Object> info) {
        try {
            String email = (String) info.get("email");
            // AHORA ESPERAMOS "paymentType" (monthly, annual, song)
            String paymentType = (String) info.get("paymentType");
            
            if (email == null || paymentType == null) {
                // Si faltan datos, lanzamos error o ponemos valores por defecto para pruebas
                throw new RuntimeException("Faltan datos: email o paymentType");
            }
            
            return this.paymentService.prepay(paymentType, email);
            
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
    // NUEVO ENDPOINT: Angular llamará a esto cuando Stripe diga "Pago OK"
    @PostMapping("/activateSubscription")
    public void activateSubscription(@RequestBody Map<String, String> info) {
        String email = info.get("email");
        if (email != null) {
            userService.activateSubscription(email);
        }
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirm(@RequestBody Map<String, Object> info) {
        try {
            String transactionId = (String) info.get("transactionId");
            if (transactionId == null || transactionId.isBlank()) {
                throw new RuntimeException("Falta transactionId");
            }
            boolean ok = paymentService.isPaymentSucceeded(transactionId);
            if (!ok) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Pago no confirmado");
            }
            return Map.of("ok", true);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}