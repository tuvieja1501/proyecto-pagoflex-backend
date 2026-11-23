package cl.duoc.pagoflex.recomendaciones;

import cl.duoc.pagoflex.recomendaciones.dto.UsuarioRemoto;
import cl.duoc.pagoflex.recomendaciones.model.Recomendacion;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Datos {

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 6, 12, 0);
    public static final LocalDate HOY = LocalDate.of(2026, 10, 6);

    private Datos() {
    }

    public static Recomendacion recomendacion(int id, int idUsuario, int idEstado) {
        Recomendacion r = new Recomendacion();
        r.setIdRecomendacion(id);
        r.setNumero("REC-000" + id);
        r.setIdUsuarioFinal(idUsuario);
        r.setNombreOrganizacion("Gimnasio Fuerza Sur");
        r.setIdRubro(11);
        r.setIdComuna(3);
        r.setFecha(AHORA.minusDays(2));
        r.setIdEstadoRecomendacion(idEstado);
        return r;
    }

    public static UsuarioRemoto usuario(int id, int rol, boolean activo) {
        return new UsuarioRemoto(id, rol, "Camila", "Rojas", activo);
    }
}
