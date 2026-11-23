package cl.duoc.pagoflex.pagos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RegistrarCompromisoRequest(
        Integer idEjecutivo,
        String rut,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        String concepto,
        Integer monto,
        LocalDate vencimiento,
        Integer cuota,
        Integer totalCuotas,
        LocalDate hoy,
        LocalDateTime fechaHora) {
}
