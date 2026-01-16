package edu.uclm.es.gramola.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping; // Import
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.model.Pricing; // Import
import edu.uclm.es.gramola.model.User;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public void register(@RequestBody Map<String, Object> datos) {
        String email = (String) datos.get("email");
        String barName = (String) datos.get("barName");
        String pwd = (String) datos.get("pwd");
        String spotifyId = (String) datos.get("spotifyId");
        String spotifySecret = (String) datos.get("spotifySecret");
        
        this.userService.register(email, barName, pwd, spotifyId, spotifySecret);
    }
    
    @GetMapping("/confirm/{token}")
    public Map<String, Object> confirm(@PathVariable String token) {
        this.userService.confirm(token);
        return Map.of("ok", true);
    }
    
    @PostMapping("/login")
    public User login(@RequestBody Map<String, Object> datos) {
        String email = (String) datos.get("email");
        String pwd = (String) datos.get("pwd");
        return this.userService.login(email, pwd);
    }

    @PostMapping("/password-reset/request")
    public Map<String, String> requestPasswordReset(@RequestBody Map<String, Object> datos) {
        String email = (String) datos.get("email");
        userService.requestPasswordReset(email);
        return Map.of("message",
                "Si el correo existe, recibirás un email con instrucciones para cambiar la contraseña.");
    }

    @PostMapping("/password-reset/confirm")
    public Map<String, String> confirmPasswordReset(@RequestBody Map<String, Object> datos) {
        String token = (String) datos.get("token");
        String newPwd = (String) datos.get("newPwd");
        userService.confirmPasswordReset(token, newPwd);
        return Map.of("message", "Contraseña actualizada correctamente");
    }

    // NUEVO ENDPOINT: Para obtener los precios de la BD
    @GetMapping("/subscription-prices")
    public List<Pricing> getSubscriptionPrices() {
        return userService.getSubscriptionPrices();
    }
}