package cl.duoc.pagoflex.recomendaciones.dto;

import java.time.LocalDateTime;

public record CambiarEstadoRequest(Integer idAgente, Integer idEstado, String motivo, LocalDateTime fechaHora) {
}
