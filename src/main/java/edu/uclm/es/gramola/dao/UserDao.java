package edu.uclm.es.gramola.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.es.gramola.model.User;

@Repository
public interface UserDao extends JpaRepository<User, String> {
    
    // Para el Login (tu código antiguo seguramente usa esto)
    User findByEmailAndPwd(String email, String pwd);

    // Para Spotify (Nuevo) -> Devuelve User directamente
    User findBySpotifyClientId(String spotifyClientId);

    // Para confirmar Email (Recuperado) -> Devuelve User directamente
    // Al quitar 'Optional', tu UserService dejará de quejarse en la línea 58
    User findByCreationTokenId(String creationTokenId);
}