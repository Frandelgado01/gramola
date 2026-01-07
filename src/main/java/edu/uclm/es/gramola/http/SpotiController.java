package edu.uclm.es.gramola.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.model.SpotiToken;
import edu.uclm.es.gramola.services.SpotiService;

@RestController
@RequestMapping("/spoti")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class SpotiController {

    @Autowired
    private SpotiService spotiService;

    @GetMapping("/getAuthorizationToken")
    public SpotiToken getAuthorizationToken(@RequestParam String code, @RequestParam String clientId) {
        return this.spotiService.getAuthorizationToken(code, clientId);
    }
}