package cl.duoc.pagoflex.recomendaciones.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RecomendacionDto(
        Integer idRecomendacion,
        String numero,
        Integer idUsuarioFinal,
        String nombreOrganizacion,
        String rutOrganizacion,
        Integer idRubro,
        Integer idComuna,
        LocalDateTime fecha,
        Integer idEstadoRecomendacion,
        ContactoDto contacto,
        List<CambioDto> cambios) {
}
