package cl.duoc.pagoflex.auth.dto;

public record RegistroRequest(
        String rut,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        Integer idComuna,
        String clave) {
}
