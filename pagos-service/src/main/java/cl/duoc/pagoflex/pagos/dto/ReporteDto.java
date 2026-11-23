package cl.duoc.pagoflex.pagos.dto;

import cl.duoc.pagoflex.pagos.model.Reporte;
import java.time.LocalDateTime;

public record ReporteDto(
        Integer idReporte,
        String numero,
        Integer idCompromiso,
        Integer idMotivoReporte,
        String comentario,
        LocalDateTime fechaCreacion,
        Integer idEstadoReporte,
        Integer idAgente,
        LocalDateTime fechaToma,
        String respuesta,
        LocalDateTime fechaResolucion) {

    public static ReporteDto desde(Reporte r) {
        return new ReporteDto(r.getIdReporte(), r.getNumero(), r.getIdCompromiso(), r.getIdMotivoReporte(),
                r.getComentario(), r.getFechaCreacion(), r.getIdEstadoReporte(), r.getIdAgente(), r.getFechaToma(),
                r.getRespuesta(), r.getFechaResolucion());
    }
}
