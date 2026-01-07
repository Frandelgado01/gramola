package edu.uclm.es.gramola.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.dao.SelectedSongDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.SelectedSong;
import edu.uclm.es.gramola.model.User;

@RestController
@RequestMapping("/gramola")
@CrossOrigin(origins = "http://127.0.0.1:4200")
public class GramolaController {

    @Autowired
    private SelectedSongDao songDao;
    
    @Autowired
    private UserDao userDao;

    // 1. INYECTAMOS EL HANDLER DE WEBSOCKETS (NUEVO)
    @Autowired
    private GramolaHandler gramolaHandler;

    @PostMapping("/add")
    public void addSong(@RequestBody Map<String, Object> info) {
        System.out.println("--- INICIO DE PETICIÓN /add ---");
        
        try {
            // 1. Recuperar datos
            String title = (String) info.get("title");
            String barId = (String) info.get("barId");
            System.out.println("1. Datos recibidos: Canción=" + title + ", BarID=" + barId);
            
            // 2. Buscar usuario
            User bar = userDao.findById(barId).isPresent() ? userDao.findById(barId).get() : null;
            
            if (bar == null) {
                System.err.println("❌ ERROR: El bar con ID " + barId + " NO existe en la base de datos.");
                throw new RuntimeException("Bar no encontrado");
            }
            System.out.println("2. Bar encontrado: " + bar.getEmail());

            // 3. Crear y guardar
            SelectedSong song = new SelectedSong();
            song.setTitle(title);
            song.setArtist((String) info.get("artist"));
            song.setSpotifyId((String) info.get("spotifyId"));
            song.setBar(bar);
            
            songDao.save(song);
            System.out.println("3. ✅ DAO save() ejecutado correctamente.");

            // 4. AVISAR POR WEBSOCKET (NUEVO)
            // Creamos un JSON manual para avisar al Front-end
            String jsonMessage = String.format(
                "{\"type\":\"NUEVA_CANCION\", \"title\":\"%s\", \"artist\":\"%s\"}", 
                song.getTitle(), song.getArtist()
            );
            
            System.out.println("📡 Enviando WebSocket: " + jsonMessage);
            gramolaHandler.broadcast(jsonMessage);
            
        } catch (Exception e) {
            System.err.println("❌ EXCEPCIÓN EN EL SERVIDOR: " + e.getMessage());
            e.printStackTrace();
            throw e; // Relanzamos para que el Frontend se entere del error
        }
        
        System.out.println("--- FIN DE PETICIÓN ---");
    }
}