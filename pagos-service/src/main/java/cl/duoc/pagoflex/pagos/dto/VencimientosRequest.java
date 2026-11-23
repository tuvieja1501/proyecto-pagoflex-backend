package cl.duoc.pagoflex.pagos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VencimientosRequest(LocalDate hoy, LocalDateTime fechaHora) {
}
