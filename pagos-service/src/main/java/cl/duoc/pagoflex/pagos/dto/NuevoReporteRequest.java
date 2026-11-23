package cl.duoc.pagoflex.pagos.dto;

import java.time.LocalDateTime;

public record NuevoReporteRequest(
        Integer idUsuario, Integer idCompromiso, Integer idMotivo, String comentario, LocalDateTime fechaHora) {
}
