package cl.duoc.pagoflex.auth.repository;

import cl.duoc.pagoflex.auth.model.Credencial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredencialRepository extends JpaRepository<Credencial, Integer> {
}
