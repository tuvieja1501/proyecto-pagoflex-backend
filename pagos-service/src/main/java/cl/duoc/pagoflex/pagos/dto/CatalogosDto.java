package cl.duoc.pagoflex.pagos.dto;

import java.util.List;

public record CatalogosDto(
        List<ItemCatalogoDto> estadosCompromiso,
        List<ItemCatalogoDto> motivosAnulacion,
        List<ItemCatalogoDto> canales,
        List<ItemCatalogoDto> mediosPago,
        List<ItemCatalogoDto> motivosReporte,
        List<ItemCatalogoDto> estadosReporte,
        List<EmpresaDto> empresas) {
}
