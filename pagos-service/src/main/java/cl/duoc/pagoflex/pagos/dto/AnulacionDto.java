package cl.duoc.pagoflex.pagos.dto;

import cl.duoc.pagoflex.pagos.model.Anulacion;
import java.time.LocalDateTime;

public record AnulacionDto(
        Integer idCompromiso,
        Integer idEjecutivo,
        Integer idMotivoAnulacion,
        String comentario,
        LocalDateTime fecha) {

    public static AnulacionDto desde(Anulacion a) {
        return new AnulacionDto(a.getIdCompromiso(), a.getIdEjecutivo(), a.getIdMotivoAnulacion(), a.getComentario(),
                a.getFecha());
    }
}
