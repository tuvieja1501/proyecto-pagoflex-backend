package cl.duoc.pagoflex.pagos.dto;

public record UsuarioRemoto(
        Integer idUsuario,
        Integer idRol,
        String rut,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        Boolean activo,
        Integer idEmpresa) {

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }
}
