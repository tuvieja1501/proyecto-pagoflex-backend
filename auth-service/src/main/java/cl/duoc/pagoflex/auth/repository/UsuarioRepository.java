package cl.duoc.pagoflex.auth.repository;

import cl.duoc.pagoflex.auth.model.Usuario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByRut(String rut);

    List<Usuario> findByIdUsuarioIn(Collection<Integer> ids);
}
