package cl.duoc.pagoflex.pagos.dto;

import java.time.LocalDateTime;

public record OtorgarReconocimientoRequest(
        Integer idUsuarioFinal, Integer idReconocimiento, LocalDateTime fechaObtencion, String motivo) {
}
