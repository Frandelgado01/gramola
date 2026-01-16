package edu.uclm.es.gramola.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.es.gramola.model.PasswordResetToken;

@Repository
public interface PasswordResetTokenDao extends JpaRepository<PasswordResetToken, String> {
}
