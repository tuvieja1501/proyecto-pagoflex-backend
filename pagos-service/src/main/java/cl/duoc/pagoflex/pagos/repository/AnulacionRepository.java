package cl.duoc.pagoflex.pagos.repository;

import cl.duoc.pagoflex.pagos.model.Anulacion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnulacionRepository extends JpaRepository<Anulacion, Integer> {

    List<Anulacion> findByIdCompromisoIn(Collection<Integer> ids);
}
