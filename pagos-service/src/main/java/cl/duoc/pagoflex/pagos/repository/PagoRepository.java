package cl.duoc.pagoflex.pagos.repository;

import cl.duoc.pagoflex.pagos.model.Pago;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    List<Pago> findByIdCompromisoIn(Collection<Integer> ids);

    boolean existsByIdCompromiso(Integer idCompromiso);

    Optional<Pago> findTopByOrderByNumeroComprobanteDesc();
}
