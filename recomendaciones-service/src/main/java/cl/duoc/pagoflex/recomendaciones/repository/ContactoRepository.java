package cl.duoc.pagoflex.recomendaciones.repository;

import cl.duoc.pagoflex.recomendaciones.model.ContactoRecomendacion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoRepository extends JpaRepository<ContactoRecomendacion, Integer> {

    List<ContactoRecomendacion> findByIdRecomendacionIn(Collection<Integer> ids);
}
