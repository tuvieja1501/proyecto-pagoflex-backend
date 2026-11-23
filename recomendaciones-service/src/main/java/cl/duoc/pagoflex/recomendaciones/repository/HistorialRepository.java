package cl.duoc.pagoflex.recomendaciones.repository;

import cl.duoc.pagoflex.recomendaciones.model.HistorialRecomendacion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialRepository extends JpaRepository<HistorialRecomendacion, Integer> {

    List<HistorialRecomendacion> findByIdRecomendacionInOrderByFecha(Collection<Integer> ids);
}
