package edu.uclm.es.gramola.services;

import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.uclm.es.gramola.dao.PricingDao;
import edu.uclm.es.gramola.dao.StripeTransactionDao;
import edu.uclm.es.gramola.model.Pricing;
import edu.uclm.es.gramola.model.StripeTransaction;

@Service
public class PaymentService {

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Autowired
    private StripeTransactionDao transactionDao;
    
    @Autowired
    private PricingDao pricingDao; // Inyectamos el DAO para leer precios

    private void ensureStripeConfigured() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new RuntimeException("Stripe no configurado (stripe.secret-key)");
        }
        Stripe.apiKey = stripeSecretKey;
    }

    // CAMBIO IMPORTANTE: Ahora recibe el TIPO de pago (ej: "monthly") en vez de la cantidad
    public StripeTransaction prepay(String paymentType, String email) throws StripeException {

        ensureStripeConfigured();
        
        // 1. Buscamos el precio en la Base de Datos
        // Si paymentType es "monthly", buscará la fila con id "monthly"
        Optional<Pricing> pricingOpt = pricingDao.findById(paymentType);
        
        if (pricingOpt.isEmpty()) {
             throw new RuntimeException("Tipo de pago no válido o no encontrado: " + paymentType);
        }
        
        Pricing pricing = pricingOpt.get();
        long amountCents = pricing.getPriceInCents(); // Convertimos a céntimos

        // 2. Decimos a Stripe que queremos cobrar esa cantidad
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setCurrency(pricing.getCurrency()) // Usamos la moneda de la BD ('eur')
                .setAmount(amountCents)
                .setDescription("Pago Gramola (" + paymentType + ") de " + email)
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        // 3. Guardamos el recibo de la transacción
        StripeTransaction transaction = new StripeTransaction();
        transaction.setAmount(amountCents);
        transaction.setEmail(email);
        
        JSONObject json = new JSONObject(intent.toJson());
        transaction.setData(json.toString());
        
        transactionDao.save(transaction);
        
        return transaction;
    }

    public boolean isPaymentSucceeded(String transactionId) throws StripeException {
        ensureStripeConfigured();

        StripeTransaction tx = transactionDao.findById(transactionId).orElse(null);
        if (tx == null) {
            throw new RuntimeException("Transacción no encontrada");
        }
        if (tx.getData() == null || tx.getData().isBlank()) {
            throw new RuntimeException("Transacción inválida");
        }

        JSONObject json = new JSONObject(tx.getData());
        String paymentIntentId = json.optString("id", null);
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new RuntimeException("PaymentIntent inválido");
        }

        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        return "succeeded".equalsIgnoreCase(intent.getStatus());
    }
}