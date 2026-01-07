package edu.uclm.es.gramola.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.uclm.es.gramola.dao.TokenDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.Token;
import edu.uclm.es.gramola.model.User;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;
    
    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private EmailService emailService;

    public void register(String email, String barName, String pwd, String spotifyId, String spotifySecret) {
        // 1. Crear y GUARDAR el token primero
        String tokenId = UUID.randomUUID().toString();
        Token token = new Token(tokenId);
        tokenDao.save(token);

        // 2. Crear el usuario vinculado a ese token
        User user = new User();
        user.setEmail(email);
        user.setBar(barName);
        user.setPwd(pwd); 
        user.setSpotifyClientId(spotifyId);
        user.setSpotifyClientSecret(spotifySecret);
        user.setCreationTokenId(tokenId);
        user.setEnabled(false); 

        // 3. Guardar el usuario
        userDao.save(user);

        // 4. Enviar correo (Simulado o real según tu EmailService)
        String link = "http://localhost:8080/users/confirm/" + tokenId;
        try {
            emailService.sendEmail(email, "Bienvenido a Gramola", 
                "Hola " + barName + ",\n\nPor favor confirma tu cuenta aquí:\n" + link);
        } catch (Exception e) {
            System.err.println("No se pudo enviar el email: " + e.getMessage());
        }
    }

    public void confirm(String tokenId) {
        // Buscamos el token (findById devuelve Optional, así que orElse(null) es correcto aquí)
        Token token = tokenDao.findById(tokenId).orElse(null);
        
        if (token == null) {
            throw new RuntimeException("Token inválido o no encontrado");
        }

        // Buscamos al usuario (findByCreationTokenId devuelve User directo, SIN orElse)
        User user = userDao.findByCreationTokenId(tokenId);
        
        if (user == null) {
            throw new RuntimeException("No hay usuario asociado a este token");
        }

        // 1. Activamos la cuenta
        user.setEnabled(true);
        
        // 2. Desvinculamos el token
        user.setCreationTokenId(null); 
        
        // 3. Guardamos al usuario actualizado
        userDao.save(user);
        
        // 4. Borramos el token usado
        tokenDao.delete(token);
    }
    
    // --- MÉTODO LOGIN CORREGIDO ---
    public User login(String email, String pwd) {
        // CORRECCIÓN: Quitamos .orElse(null) porque el DAO ya devuelve User
        User user = userDao.findByEmailAndPwd(email, pwd);

        // Si no existe, error
        if (user == null) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        // Si existe pero no ha confirmado el correo, error
        if (!user.isEnabled()) {
            throw new RuntimeException("Debes confirmar tu correo primero");
        }
        
        return user;
    }
}