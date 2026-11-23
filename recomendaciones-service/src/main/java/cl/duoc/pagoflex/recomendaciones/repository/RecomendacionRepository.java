package cl.duoc.pagoflex.recomendaciones.repository;

import cl.duoc.pagoflex.recomendaciones.model.Recomendacion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecomendacionRepository extends JpaRepository<Recomendacion, Integer> {

    List<Recomendacion> findByIdUsuarioFinalOrderByFechaDesc(Integer idUsuarioFinal);

    List<Recomendacion> findAllByOrderByFechaDesc();

    Optional<Recomendacion> findByIdUsuarioFinalAndNombreOrganizacionIgnoreCase(Integer idUsuarioFinal, String nombre);

    long countByIdUsuarioFinalAndFechaGreaterThanEqualAndFechaLessThan(
            Integer idUsuarioFinal, LocalDateTime desde, LocalDateTime hasta);

    long countByIdUsuarioFinalAndIdEstadoRecomendacion(Integer idUsuarioFinal, Integer idEstado);

    Optional<Recomendacion> findTopByOrderByNumeroDesc();
}
