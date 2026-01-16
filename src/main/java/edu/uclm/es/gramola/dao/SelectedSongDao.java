package edu.uclm.es.gramola.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.es.gramola.model.SelectedSong;

@Repository
public interface SelectedSongDao extends JpaRepository<SelectedSong, Integer> {
	java.util.List<SelectedSong> findAllByOrderByIdAsc();
}