package cl.duoc.pagoflex.auth.dto;

import java.time.LocalDateTime;

public record CodigoRecuperacionDto(String nombre, String codigo, LocalDateTime expira) {
}
