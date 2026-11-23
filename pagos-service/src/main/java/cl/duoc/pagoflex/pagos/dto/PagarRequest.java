package cl.duoc.pagoflex.pagos.dto;

import java.time.LocalDateTime;

public record PagarRequest(Integer idUsuario, Integer idMedioPago, Integer idCanal, LocalDateTime fechaHora) {
}
