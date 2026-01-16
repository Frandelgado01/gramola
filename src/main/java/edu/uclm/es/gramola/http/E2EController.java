package edu.uclm.es.gramola.http;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.dao.PricingDao;
import edu.uclm.es.gramola.dao.SelectedSongDao;
import edu.uclm.es.gramola.dao.StripeTransactionDao;
import edu.uclm.es.gramola.model.Pricing;
import edu.uclm.es.gramola.model.SelectedSong;
import edu.uclm.es.gramola.model.StripeTransaction;

@RestController
@RequestMapping("/e2e")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" })
@ConditionalOnProperty(name = "e2e.enabled", havingValue = "true")
public class E2EController {

    @Autowired
    private StripeTransactionDao stripeTransactionDao;

    @Autowired
    private SelectedSongDao selectedSongDao;

    @Autowired
    private PricingDao pricingDao;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true);
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        selectedSongDao.deleteAll();
        stripeTransactionDao.deleteAll();

        seedPricingIfMissing("song", 0.50, "eur");
        seedPricingIfMissing("monthly", 9.99, "eur");
        seedPricingIfMissing("annual", 99.99, "eur");

        return Map.of(
                "ok", true,
                "songs", selectedSongDao.count(),
                "transactions", stripeTransactionDao.count());
    }

    @GetMapping("/state")
    public Map<String, Object> state(@RequestParam(required = false) String email) {
        List<SelectedSong> songs = selectedSongDao.findAllByOrderByIdAsc();

        List<StripeTransaction> txs = stripeTransactionDao.findAll();
        if (email != null && !email.isBlank()) {
            txs = txs.stream().filter(tx -> Objects.equals(email, tx.getEmail())).toList();
        }

        List<String> txIds = txs.stream()
                .map(StripeTransaction::getId)
                .sorted(Comparator.naturalOrder())
                .toList();

        return Map.of(
                "ok", true,
                "songCount", songs.size(),
                "songs", songs,
                "transactionCount", txs.size(),
                "transactionIds", txIds);
    }

    private void seedPricingIfMissing(String type, double price, String currency) {
        if (pricingDao.existsById(type)) {
            return;
        }

        Pricing pricing = new Pricing();
        pricing.setType(type);
        pricing.setPrice(price);
        pricing.setCurrency(currency);
        pricingDao.save(pricing);
    }
}
