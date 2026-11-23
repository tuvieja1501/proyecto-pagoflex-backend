package cl.duoc.pagoflex.auth.dto;

public record RestablecerRequest(String correo, String codigo, String nuevaClave) {
}
