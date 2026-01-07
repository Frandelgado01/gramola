package edu.uclm.es.gramola.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.es.gramola.model.SelectedSong;

@Repository
public interface SelectedSongDao extends JpaRepository<SelectedSong, String> {
    // Nos servirá para listar el historial de un bar concreto
    List<SelectedSong> findByBarEmail(String email);
}