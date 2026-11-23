package cl.duoc.pagoflex.recomendaciones.repository;

import cl.duoc.pagoflex.recomendaciones.model.Catalogo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoRepository extends JpaRepository<Catalogo, Integer> {

    List<Catalogo> findByTipoOrderByCodigo(String tipo);

    Optional<Catalogo> findByTipoAndCodigo(String tipo, Integer codigo);

    boolean existsByTipoAndCodigo(String tipo, Integer codigo);
}
