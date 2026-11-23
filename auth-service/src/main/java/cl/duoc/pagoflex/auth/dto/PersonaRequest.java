package cl.duoc.pagoflex.auth.dto;

public record PersonaRequest(String rut, String nombres, String apellidos, String correo, String telefono) {
}
