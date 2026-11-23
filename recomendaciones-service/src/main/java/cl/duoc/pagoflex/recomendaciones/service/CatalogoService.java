package cl.duoc.pagoflex.recomendaciones.service;

import cl.duoc.pagoflex.recomendaciones.dto.CatalogosDto;
import cl.duoc.pagoflex.recomendaciones.dto.ItemCatalogoDto;
import cl.duoc.pagoflex.recomendaciones.repository.CatalogoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogoService {

    private final CatalogoRepository catalogos;

    public CatalogoService(CatalogoRepository catalogos) {
        this.catalogos = catalogos;
    }

    private List<ItemCatalogoDto> items(String tipo) {
        return catalogos.findByTipoOrderByCodigo(tipo).stream()
                .map(c -> new ItemCatalogoDto(c.getCodigo(), c.getNombre()))
                .toList();
    }

    public CatalogosDto todos() {
        return new CatalogosDto(items(ReglasRecomendacion.TIPO_RUBRO), items(ReglasRecomendacion.TIPO_ESTADO));
    }
}
