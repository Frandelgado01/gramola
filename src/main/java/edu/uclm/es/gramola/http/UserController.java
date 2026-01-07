package edu.uclm.es.gramola.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.model.User; // <--- IMPORTANTE: Hemos añadido esto
import edu.uclm.es.gramola.services.UserService;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public void register(@RequestBody Map<String, Object> info) {
        String email = (String) info.get("email");
        String bar = (String) info.get("bar");
        String pwd1 = (String) info.get("pwd1");
        String pwd2 = (String) info.get("pwd2");
        String spotifyId = (String) info.get("clientId");
        String spotifySecret = (String) info.get("clientSecret");

        if (!pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Las contraseñas no coinciden");
        }

        try {
            this.userService.register(email, bar, pwd1, spotifyId, spotifySecret);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Error al registrar: " + e.getMessage());
        }
    }

    @GetMapping("/confirm/{tokenId}")
    public String confirm(@PathVariable String tokenId) {
        try {
            this.userService.confirm(tokenId);
            return "<h1>¡Cuenta activada con éxito!</h1> <p>Ya puedes volver a la web y entrar.</p>";
        } catch (Exception e) {
            return "<h1>Error</h1> <p>" + e.getMessage() + "</p>";
        }
    }
    
    // --- NUEVO MÉTODO DE LOGIN ---
    @PostMapping("/login")
    public User login(@RequestBody Map<String, Object> info) {
        String email = (String) info.get("email");
        String pwd = (String) info.get("pwd");
        
        try {
            return this.userService.login(email, pwd);
        } catch (Exception e) {
            // Si la contraseña está mal o el usuario no está activo, devolvemos error 403
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
}