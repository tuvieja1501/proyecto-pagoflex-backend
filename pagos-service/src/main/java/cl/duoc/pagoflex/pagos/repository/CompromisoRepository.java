package cl.duoc.pagoflex.pagos.repository;

import cl.duoc.pagoflex.pagos.model.Compromiso;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompromisoRepository extends JpaRepository<Compromiso, Integer> {

    List<Compromiso> findByIdUsuarioFinalOrderByFechaVencimiento(Integer idUsuarioFinal);

    List<Compromiso> findByIdEmpresaOrderByFechaVencimiento(Integer idEmpresa);

    List<Compromiso> findByIdEstadoCompromisoAndFechaVencimientoBefore(Integer idEstado, LocalDate fecha);

    boolean existsByIdUsuarioFinalAndIdEmpresaAndConceptoAndNumeroCuota(
            Integer idUsuarioFinal, Integer idEmpresa, String concepto, Integer numeroCuota);

    Optional<Compromiso> findTopByFolioStartingWithOrderByFolioDesc(String prefijo);
}
