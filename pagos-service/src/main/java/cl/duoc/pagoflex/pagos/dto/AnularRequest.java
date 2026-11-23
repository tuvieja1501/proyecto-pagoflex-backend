package cl.duoc.pagoflex.pagos.dto;

import java.time.LocalDateTime;

public record AnularRequest(Integer idEjecutivo, Integer idMotivo, String comentario, LocalDateTime fechaHora) {
}
