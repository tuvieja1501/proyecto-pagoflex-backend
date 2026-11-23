package cl.duoc.pagoflex.recomendaciones.dto;

import java.time.LocalDateTime;

public record NuevaRecomendacionRequest(
        Integer idUsuario,
        String organizacion,
        String rutOrganizacion,
        Integer idRubro,
        Integer idComuna,
        String contactoNombre,
        String contactoCorreo,
        String contactoTelefono,
        Boolean autorizaContacto,
        LocalDateTime fechaHora) {
}
