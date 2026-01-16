package edu.uclm.es.gramola.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.uclm.es.gramola.dao.PasswordResetTokenDao;
import edu.uclm.es.gramola.dao.PricingDao; // Import nuevo
import edu.uclm.es.gramola.dao.TokenDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.PasswordResetToken;
import edu.uclm.es.gramola.model.Pricing;  // Import nuevo
import edu.uclm.es.gramola.model.Token;
import edu.uclm.es.gramola.model.User;

@Service
public class UserService {

    private static final long PASSWORD_RESET_TTL_MS = 30L * 60L * 1000L; // 30 min

    @Autowired
    private UserDao userDao;
    
    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private PasswordResetTokenDao passwordResetTokenDao;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PricingDao pricingDao; // NUEVO: Para leer los precios

    @Value("${app.frontend.base-url:http://127.0.0.1:4200}")
    private String frontendBaseUrl;

    public void register(String email, String barName, String pwd, String spotifyId, String spotifySecret) {
        String tokenId = UUID.randomUUID().toString();
        Token token = new Token(tokenId);
        tokenDao.save(token);

        User user = new User();
        user.setEmail(email);
        user.setBar(barName);
        user.setPwd(pwd); 
        user.setSpotifyClientId(spotifyId);
        user.setSpotifyClientSecret(spotifySecret);
        user.setCreationTokenId(tokenId);
        user.setEnabled(false);
        user.setSubscriptionActive(false); // Por defecto no ha pagado

        userDao.save(user);

        String link = frontendBaseUrl + "/confirmar?token=" + tokenId + "&email=" +
                URLEncoder.encode(email, StandardCharsets.UTF_8);
        try {
            emailService.sendEmail(email, "Bienvenido a Gramola", 
                "Hola " + barName + ",\n\nPor favor confirma tu cuenta aquí:\n" + link);
        } catch (Exception e) {
            System.err.println("No se pudo enviar el email: " + e.getMessage());
        }
    }

    public void confirm(String tokenId) {
        Token token = tokenDao.findById(tokenId).orElse(null);
        if (token == null) throw new RuntimeException("Token inválido o no encontrado");

        User user = userDao.findByCreationTokenId(tokenId);
        if (user == null) throw new RuntimeException("No hay usuario asociado a este token");

        user.setEnabled(true);
        user.setCreationTokenId(null); 
        userDao.save(user);
        tokenDao.delete(token);
    }
    
    public User login(String email, String pwd) {
        User user = userDao.findByEmailAndPwd(email, pwd);

        if (user == null) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Debes confirmar tu correo primero");
        }
        
        // NOTA: Aquí NO lanzamos error si no ha pagado. 
        // Devolvemos el usuario tal cual, y el Angular decidirá si le manda a pagar.
        return user;
    }

    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email requerido");
        }

        User user = userDao.findByEmail(email);
        if (user == null) {
            // No revelamos si existe o no
            return;
        }

        String tokenId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long expiresAt = now + PASSWORD_RESET_TTL_MS;

        PasswordResetToken token = new PasswordResetToken(tokenId, email, expiresAt);
        passwordResetTokenDao.save(token);

        String link = frontendBaseUrl + "/cambiar-clave?token=" + tokenId;
        try {
            emailService.sendEmail(email, "Recuperar contraseña - Gramola",
                    "Has solicitado cambiar tu contraseña.\n\n" +
                    "Usa este enlace para establecer una nueva contraseña:\n" + link +
                    "\n\nSi no has sido tú, ignora este correo.\n" +
                    "(El enlace caduca en 30 minutos)"
            );
        } catch (Exception e) {
            System.err.println("No se pudo enviar el email de recuperación: " + e.getMessage());
        }
    }

    public void confirmPasswordReset(String tokenId, String newPwd) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new RuntimeException("Token requerido");
        }
        if (newPwd == null || newPwd.isBlank()) {
            throw new RuntimeException("Nueva contraseña requerida");
        }

        PasswordResetToken token = passwordResetTokenDao.findById(tokenId).orElse(null);
        if (token == null) {
            throw new RuntimeException("Token inválido");
        }
        if (token.getUseTime() != null) {
            throw new RuntimeException("Token ya usado");
        }
        if (System.currentTimeMillis() > token.getExpiresAt()) {
            throw new RuntimeException("Token caducado");
        }

        User user = userDao.findByEmail(token.getEmail());
        if (user == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        user.setPwd(newPwd);
        userDao.save(user);

        token.setUseTime(System.currentTimeMillis());
        passwordResetTokenDao.save(token);
    }

    // --- NUEVOS MÉTODOS PARA FASE 2 ---

    // 1. Activa la suscripción (se llama desde PaymentController)
    public void activateSubscription(String email) {
        User user = userDao.findByEmail(email);
        if (user != null) {
            user.setSubscriptionActive(true);
            userDao.save(user);
        }
    }

    // 2. Devuelve los precios para que Angular los pinte
    public List<Pricing> getSubscriptionPrices() {
        return pricingDao.findAll();
    }
}