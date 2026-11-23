package cl.duoc.pagoflex.pagos;

import cl.duoc.pagoflex.pagos.dto.UsuarioRemoto;
import cl.duoc.pagoflex.pagos.model.Compromiso;
import cl.duoc.pagoflex.pagos.model.Reporte;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Datos {

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 6, 12, 0);
    public static final LocalDate HOY = LocalDate.of(2026, 10, 6);

    private Datos() {
    }

    public static Compromiso compromiso(int id, int idUsuario, int idEstado, LocalDate vencimiento) {
        Compromiso c = new Compromiso();
        c.setIdCompromiso(id);
        c.setFolio("CP-2026-000" + id);
        c.setIdUsuarioFinal(idUsuario);
        c.setIdEmpresa(1);
        c.setConcepto("Cuota arriendo");
        c.setNumeroCuota(2);
        c.setTotalCuotas(6);
        c.setMonto(50_000);
        c.setFechaVencimiento(vencimiento);
        c.setIdEstadoCompromiso(idEstado);
        c.setRecargoAplicado(0);
        c.setFechaCambioEstado(AHORA.minusDays(10));
        c.setFechaRegistro(AHORA.minusDays(30));
        return c;
    }

    public static Reporte reporte(int id, int idCompromiso, int idEstado) {
        Reporte r = new Reporte();
        r.setIdReporte(id);
        r.setNumero("REP-000" + id);
        r.setIdCompromiso(idCompromiso);
        r.setIdMotivoReporte(1);
        r.setFechaCreacion(AHORA.minusDays(1));
        r.setIdEstadoReporte(idEstado);
        return r;
    }

    public static UsuarioRemoto usuario(int id, int rol, Integer idEmpresa) {
        return new UsuarioRemoto(id, rol, "12.345.678-5", "Camila", "Rojas", "c@correo.ejemplo", "+56912345678",
                true, idEmpresa);
    }
}
