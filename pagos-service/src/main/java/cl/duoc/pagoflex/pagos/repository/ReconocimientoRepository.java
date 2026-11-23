package cl.duoc.pagoflex.pagos.repository;

import cl.duoc.pagoflex.pagos.model.UsuarioReconocimiento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconocimientoRepository extends JpaRepository<UsuarioReconocimiento, Integer> {

    List<UsuarioReconocimiento> findByIdUsuarioFinal(Integer idUsuarioFinal);

    Optional<UsuarioReconocimiento> findByIdUsuarioFinalAndIdReconocimiento(Integer idUsuarioFinal, Integer idReconocimiento);
}
