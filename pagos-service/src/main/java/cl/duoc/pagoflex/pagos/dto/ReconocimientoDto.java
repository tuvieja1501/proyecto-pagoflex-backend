package cl.duoc.pagoflex.pagos.dto;

import cl.duoc.pagoflex.pagos.model.UsuarioReconocimiento;
import java.time.LocalDateTime;

public record ReconocimientoDto(
        Integer idUsuarioFinal,
        Integer idReconocimiento,
        LocalDateTime fechaObtencion,
        String motivo) {

    public static ReconocimientoDto desde(UsuarioReconocimiento r) {
        return new ReconocimientoDto(r.getIdUsuarioFinal(), r.getIdReconocimiento(), r.getFechaObtencion(), r.getMotivo());
    }
}
