package cl.duoc.pagoflex.pagos.repository;

import cl.duoc.pagoflex.pagos.model.Reporte;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReporteRepository extends JpaRepository<Reporte, Integer> {

    List<Reporte> findByIdCompromisoIn(Collection<Integer> ids);

    Optional<Reporte> findFirstByIdCompromisoAndIdEstadoReporteNot(Integer idCompromiso, Integer idEstado);

    Optional<Reporte> findTopByOrderByNumeroDesc();
}
