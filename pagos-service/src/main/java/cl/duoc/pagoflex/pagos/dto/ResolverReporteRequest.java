package cl.duoc.pagoflex.pagos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResolverReporteRequest(Integer idAgente, String respuesta, LocalDate hoy, LocalDateTime fechaHora) {
}
