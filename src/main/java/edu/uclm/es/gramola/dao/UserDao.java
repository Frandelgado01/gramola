package edu.uclm.es.gramola.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.es.gramola.model.User;

@Repository
public interface UserDao extends JpaRepository<User, String> {
    
    // 1. Para confirmar cuenta (El token es único, usamos findBy normal)
    User findByCreationTokenId(String creationTokenId);

    // 2. Para Login (El email es único, usamos findBy normal)
    User findByEmailAndPwd(String email, String pwd);

    // 3. Para conectar Spotify (HAY DUPLICADOS, usamos findFirst para evitar el error)
    User findFirstBySpotifyClientId(String spotifyClientId);

    User findByEmail(String email);
}