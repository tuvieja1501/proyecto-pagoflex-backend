package cl.duoc.pagoflex.pagos.dto;

import cl.duoc.pagoflex.pagos.model.Compromiso;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompromisoDto(
        Integer idCompromiso,
        String folio,
        Integer idUsuarioFinal,
        Integer idEmpresa,
        String concepto,
        Integer numeroCuota,
        Integer totalCuotas,
        Integer monto,
        LocalDate fechaVencimiento,
        Integer idEstadoCompromiso,
        Integer recargoAplicado,
        LocalDateTime fechaCambioEstado,
        Integer idEjecutivoRegistro,
        LocalDateTime fechaRegistro) {

    public static CompromisoDto desde(Compromiso c) {
        return new CompromisoDto(c.getIdCompromiso(), c.getFolio(), c.getIdUsuarioFinal(), c.getIdEmpresa(),
                c.getConcepto(), c.getNumeroCuota(), c.getTotalCuotas(), c.getMonto(), c.getFechaVencimiento(),
                c.getIdEstadoCompromiso(), c.getRecargoAplicado(), c.getFechaCambioEstado(),
                c.getIdEjecutivoRegistro(), c.getFechaRegistro());
    }
}
