package edu.uclm.es.gramola.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import edu.uclm.es.gramola.dao.StripeTransactionDao;
import edu.uclm.es.gramola.model.StripeTransaction;
import org.json.JSONObject;
import jakarta.annotation.PostConstruct;

@Service
public class PaymentService {

    @Value("${stripe.secret}")
    private String stripeSecret;

    @PostConstruct
    void initStripe() {
        Stripe.apiKey = stripeSecret;
    }

    @Autowired
    private StripeTransactionDao transactionDao;

    public StripeTransaction prepay(long amountCents, String email) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setCurrency("eur")
                .setAmount(amountCents)
                .setDescription("Pago Gramola de " + email)
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        StripeTransaction transaction = new StripeTransaction();
        transaction.setAmount(amountCents);
        transaction.setEmail(email);

        JSONObject json = new JSONObject(intent.toJson());
        transaction.setData(json.toString());

        transactionDao.save(transaction);

        return transaction;
    }
}