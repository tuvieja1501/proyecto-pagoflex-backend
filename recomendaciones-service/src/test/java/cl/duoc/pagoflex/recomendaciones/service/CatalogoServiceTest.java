package cl.duoc.pagoflex.recomendaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cl.duoc.pagoflex.recomendaciones.dto.CatalogosDto;
import cl.duoc.pagoflex.recomendaciones.model.Catalogo;
import cl.duoc.pagoflex.recomendaciones.repository.CatalogoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogoServiceTest {

    @Test
    void agrupaRubrosYEstados() {
        CatalogoRepository repo = mock(CatalogoRepository.class);
        when(repo.findByTipoOrderByCodigo(ReglasRecomendacion.TIPO_RUBRO))
                .thenReturn(List.of(new Catalogo(1, ReglasRecomendacion.TIPO_RUBRO, 11, "Deporte")));
        when(repo.findByTipoOrderByCodigo(ReglasRecomendacion.TIPO_ESTADO))
                .thenReturn(List.of(new Catalogo(2, ReglasRecomendacion.TIPO_ESTADO, 1, "Recibida"),
                        new Catalogo(3, ReglasRecomendacion.TIPO_ESTADO, 2, "Contactada")));
        CatalogosDto dto = new CatalogoService(repo).todos();
        assertEquals("Deporte", dto.rubros().get(0).nombre());
        assertEquals(2, dto.estados().size());
        assertEquals(2, dto.estados().get(1).id());
    }
}
