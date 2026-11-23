package cl.duoc.pagoflex.pagos.service;

import cl.duoc.pagoflex.pagos.dto.OtorgarReconocimientoRequest;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import cl.duoc.pagoflex.pagos.model.UsuarioReconocimiento;
import cl.duoc.pagoflex.pagos.repository.ReconocimientoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconocimientoService {

    private final ReconocimientoRepository reconocimientos;
    private final Clock reloj;

    public ReconocimientoService(ReconocimientoRepository reconocimientos, Clock reloj) {
        this.reconocimientos = reconocimientos;
        this.reloj = reloj;
    }

    public List<UsuarioReconocimiento> listar(Integer idUsuario) {
        return reconocimientos.findByIdUsuarioFinal(idUsuario);
    }

    @Transactional
    public UsuarioReconocimiento otorgar(OtorgarReconocimientoRequest p) {
        if (p.idUsuarioFinal() == null || p.idReconocimiento() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Indica la persona y el reconocimiento.");
        }
        if (p.motivo() == null || p.motivo().isBlank()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Indica el motivo del reconocimiento.");
        }
        return reconocimientos.findByIdUsuarioFinalAndIdReconocimiento(p.idUsuarioFinal(), p.idReconocimiento())
                .orElseGet(() -> {
                    UsuarioReconocimiento nuevo = new UsuarioReconocimiento();
                    nuevo.setIdUsuarioFinal(p.idUsuarioFinal());
                    nuevo.setIdReconocimiento(p.idReconocimiento());
                    nuevo.setFechaObtencion(p.fechaObtencion() != null ? p.fechaObtencion() : LocalDateTime.now(reloj));
                    nuevo.setMotivo(p.motivo().trim());
                    return reconocimientos.save(nuevo);
                });
    }
}
