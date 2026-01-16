package edu.uclm.es.gramola.http;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping; // <--- IMPORTANTE
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.dao.SelectedSongDao;
import edu.uclm.es.gramola.model.SelectedSong;
import edu.uclm.es.gramola.services.PaymentService;

@RestController
@RequestMapping("/gramola")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class GramolaController {

    @Autowired
    private SelectedSongDao songDao;

    @Autowired
    private GramolaHandler gramolaHandler; 

    @Autowired
    private PaymentService paymentService;

    // --- 1. AÑADIR CANCIÓN (PAGO REALIZADO) ---
    @PostMapping("/add-song")
    public ResponseEntity<Map<String, String>> addSong(@RequestBody Map<String, Object> trackData) {
        System.out.println("--- 🎵 RECIBIENDO CANCIÓN PAGADA ---");
        
        try {
            // Validar pago (si viene transactionId)
            String transactionId = null;
            try {
                transactionId = (String) trackData.get("transactionId");
            } catch (Exception ignore) {
            }
            if (transactionId == null || transactionId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Falta transactionId");
            }
            boolean paid = paymentService.isPaymentSucceeded(transactionId);
            if (!paid) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Pago no confirmado");
            }

            String name = (String) trackData.get("name");
            String id = (String) trackData.get("id");
            
            String artistName = "Desconocido";
            try {
                List<Map<String, Object>> artists = (List<Map<String, Object>>) trackData.get("artists");
                if (artists != null && !artists.isEmpty()) {
                    artistName = (String) artists.get(0).get("name");
                }
            } catch (Exception e) {
                System.out.println("No se pudo leer el artista, usando default.");
            }

            // Guardar en BD
            SelectedSong song = new SelectedSong();
            song.setTitle(name);
            song.setArtist(artistName);
            song.setSpotifyId(id);
            
            songDao.save(song);
            System.out.println("✅ Canción guardada en la cola (ID: " + song.getId() + ")");

            // Avisar por WebSocket
            if (gramolaHandler != null) {
                String jsonMessage = String.format(
                    "{\"type\":\"NUEVA_CANCION\", \"id\":%d, \"spotifyId\":\"%s\", \"title\":\"%s\", \"artist\":\"%s\"}", 
                    song.getId(),
                    song.getSpotifyId() != null ? song.getSpotifyId() : "",
                    song.getTitle(),
                    song.getArtist()
                );
                System.out.println("📡 Enviando WebSocket: " + jsonMessage);
                gramolaHandler.broadcast(jsonMessage);
            }

            return ResponseEntity.ok(Collections.singletonMap("mensaje", "Canción guardada correctamente"));

        } catch (Exception e) {
            if (e instanceof ResponseStatusException rse) {
                return ResponseEntity.status(rse.getStatusCode())
                        .body(Collections.singletonMap("error", rse.getReason() != null ? rse.getReason() : "Error"));
            }
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", "Error en el servidor"));
        }
    }

    // --- 2. LEER LA COLA (PARA CUANDO ENTRAS AL HOME) ---
    // ¡ESTE ES EL MÉTODO QUE TE FALTABA!
    @GetMapping("/queue")
    public List<SelectedSong> getQueue() {
        System.out.println("📥 Alguien ha entrado al Home y pide la lista de canciones...");
        return songDao.findAllByOrderByIdAsc();
    }

    // --- 3. BORRAR UNA CANCIÓN DE LA COLA (CUANDO EMPIEZA A SONAR) ---
    @DeleteMapping("/queue/{id}")
    public ResponseEntity<Map<String, String>> deleteFromQueue(@PathVariable int id) {
        if (!songDao.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "No existe la canción en cola"));
        }
        songDao.deleteById(id);

        // Avisar por WebSocket para refrescar la cola en otros clientes
        if (gramolaHandler != null) {
            String jsonMessage = String.format("{\"type\":\"COLA_ACTUALIZADA\", \"deletedId\":%d}", id);
            gramolaHandler.broadcast(jsonMessage);
        }

        return ResponseEntity.ok(Collections.singletonMap("mensaje", "Eliminada"));
    }

    // --- 4. LIMPIAR CANCIONES OBSOLETAS (SINCRONIZAR CON SPOTIFY) ---
    // El frontend calcula qué IDs ya no están en la cola real de Spotify y pide borrarlos.
    @PostMapping("/queue/prune")
    public ResponseEntity<Map<String, Object>> pruneQueue(@RequestBody Map<String, Object> body) {
        Object idsObj = body != null ? body.get("ids") : null;
        if (!(idsObj instanceof List<?> rawIds)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Falta 'ids' (array de enteros)"));
        }

        int deleted = 0;
        for (Object raw : rawIds) {
            Integer id = null;
            if (raw instanceof Number n) {
                id = n.intValue();
            } else if (raw instanceof String s) {
                try {
                    id = Integer.parseInt(s);
                } catch (NumberFormatException ignore) {
                }
            }

            if (id != null && songDao.existsById(id)) {
                songDao.deleteById(id);
                deleted++;
            }
        }

        if (deleted > 0 && gramolaHandler != null) {
            String jsonMessage = String.format("{\"type\":\"COLA_ACTUALIZADA\", \"pruned\":%d}", deleted);
            gramolaHandler.broadcast(jsonMessage);
        }

        return ResponseEntity.ok(Map.of(
                "deleted", deleted,
                "requested", rawIds.size()
        ));
    }
}