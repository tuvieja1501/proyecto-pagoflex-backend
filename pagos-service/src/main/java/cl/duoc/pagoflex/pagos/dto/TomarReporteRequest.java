package cl.duoc.pagoflex.pagos.dto;

import java.time.LocalDateTime;

public record TomarReporteRequest(Integer idAgente, LocalDateTime fechaHora) {
}
