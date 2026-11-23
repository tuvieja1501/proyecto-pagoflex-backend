package cl.duoc.pagoflex.pagos.dto;

import cl.duoc.pagoflex.pagos.model.EmpresaCliente;

public record EmpresaDto(
        Integer idEmpresa,
        String codigo,
        String razonSocial,
        String nombreFantasia,
        String rut,
        Integer idRubro,
        String correoAtencion,
        String telefonoAtencion,
        Integer recargoAtraso,
        Boolean activa) {

    public static EmpresaDto desde(EmpresaCliente e) {
        return new EmpresaDto(e.getIdEmpresa(), e.getCodigo(), e.getRazonSocial(), e.getNombreFantasia(), e.getRut(),
                e.getIdRubro(), e.getCorreoAtencion(), e.getTelefonoAtencion(), e.getRecargoAtraso(), e.getActiva());
    }
}
