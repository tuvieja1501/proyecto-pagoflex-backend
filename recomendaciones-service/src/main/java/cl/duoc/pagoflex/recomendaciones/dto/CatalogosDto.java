package cl.duoc.pagoflex.recomendaciones.dto;

import java.util.List;

public record CatalogosDto(List<ItemCatalogoDto> rubros, List<ItemCatalogoDto> estados) {
}
