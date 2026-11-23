package cl.duoc.pagoflex.pagos.dto;

import cl.duoc.pagoflex.pagos.model.Pago;
import java.time.LocalDateTime;

public record PagoDto(
        Integer idPago,
        String numeroComprobante,
        Integer idCompromiso,
        LocalDateTime fechaHora,
        Integer montoPagado,
        Integer idCanal,
        Integer idMedioPago) {

    public static PagoDto desde(Pago p) {
        return new PagoDto(p.getIdPago(), p.getNumeroComprobante(), p.getIdCompromiso(), p.getFechaHora(),
                p.getMontoPagado(), p.getIdCanal(), p.getIdMedioPago());
    }
}
