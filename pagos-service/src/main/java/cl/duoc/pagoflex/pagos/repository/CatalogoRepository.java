package cl.duoc.pagoflex.pagos.repository;

import cl.duoc.pagoflex.pagos.model.Catalogo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoRepository extends JpaRepository<Catalogo, Integer> {

    List<Catalogo> findByTipoOrderByCodigo(String tipo);

    boolean existsByTipoAndCodigo(String tipo, Integer codigo);
}
