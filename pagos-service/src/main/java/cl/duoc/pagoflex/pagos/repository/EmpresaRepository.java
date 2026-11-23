package cl.duoc.pagoflex.pagos.repository;

import cl.duoc.pagoflex.pagos.model.EmpresaCliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<EmpresaCliente, Integer> {
}
