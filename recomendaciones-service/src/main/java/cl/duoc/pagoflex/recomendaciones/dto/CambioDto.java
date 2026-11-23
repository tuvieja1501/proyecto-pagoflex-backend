package cl.duoc.pagoflex.recomendaciones.dto;

import java.time.LocalDateTime;

public record CambioDto(Integer idEstado, Integer idAgente, LocalDateTime fecha, String motivo) {
}
