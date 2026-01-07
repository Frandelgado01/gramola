package edu.uclm.es.gramola.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.SpotiToken;
import edu.uclm.es.gramola.model.User;

@Service
public class SpotiService {

    @Autowired
    private UserDao userDao;

    // Este es el método que tu Controller no encontraba
    public SpotiToken getAuthorizationToken(String code, String clientId) {
        
        // 1. Buscamos el usuario
        User user = userDao.findBySpotifyClientId(clientId);
        
        if (user == null) {
            throw new RuntimeException("No existe usuario con Client ID: " + clientId);
        }

        // 2. Obtenemos el secreto
        String clientSecret = user.getSpotifyClientSecret();

        // 3. Preparamos la llamada a Spotify
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret); 

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", "http://127.0.0.1:4200/home"); 

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // 4. Hacemos el POST (URL oficial de Spotify)
        String spotifyUrl = "https://accounts.spotify.com/api/token";
            
        try {
            ResponseEntity<SpotiToken> response = restTemplate.postForEntity(
                spotifyUrl,
                request,
                SpotiToken.class
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error Spotify: " + e.getMessage());
            return null; // O lanzar la excepción
        }
    }
}