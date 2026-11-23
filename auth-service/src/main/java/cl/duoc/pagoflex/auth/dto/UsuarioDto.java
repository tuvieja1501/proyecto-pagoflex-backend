package cl.duoc.pagoflex.auth.dto;

import cl.duoc.pagoflex.auth.model.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UsuarioDto(
        Integer idUsuario,
        Integer idRol,
        String rut,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        Boolean activo,
        LocalDateTime fechaCreacion,
        Integer idComuna,
        LocalDate fechaIncorporacion,
        Boolean avisosActivos,
        Integer diasAnticipacion,
        Integer idArea,
        Integer idEmpresa,
        String cargo) {

    public static UsuarioDto desde(Usuario u) {
        return new UsuarioDto(u.getIdUsuario(), u.getIdRol(), u.getRut(), u.getNombres(), u.getApellidos(),
                u.getCorreo(), u.getTelefono(), u.getActivo(), u.getFechaCreacion(), u.getIdComuna(),
                u.getFechaIncorporacion(), u.getAvisosActivos(), u.getDiasAnticipacion(), u.getIdArea(),
                u.getIdEmpresa(), u.getCargo());
    }
}
