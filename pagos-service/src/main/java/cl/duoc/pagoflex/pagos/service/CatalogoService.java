package cl.duoc.pagoflex.pagos.service;

import cl.duoc.pagoflex.pagos.dto.CatalogosDto;
import cl.duoc.pagoflex.pagos.dto.EmpresaDto;
import cl.duoc.pagoflex.pagos.dto.ItemCatalogoDto;
import cl.duoc.pagoflex.pagos.repository.CatalogoRepository;
import cl.duoc.pagoflex.pagos.repository.EmpresaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogoService {

    private final CatalogoRepository catalogos;
    private final EmpresaRepository empresas;

    public CatalogoService(CatalogoRepository catalogos, EmpresaRepository empresas) {
        this.catalogos = catalogos;
        this.empresas = empresas;
    }

    private List<ItemCatalogoDto> items(String tipo) {
        return catalogos.findByTipoOrderByCodigo(tipo).stream()
                .map(c -> new ItemCatalogoDto(c.getCodigo(), c.getNombre()))
                .toList();
    }

    public CatalogosDto todos() {
        List<EmpresaDto> listaEmpresas = empresas.findAll().stream().map(EmpresaDto::desde).toList();
        return new CatalogosDto(
                items(ReglasPagos.TIPO_ESTADO_COMPROMISO),
                items(ReglasPagos.TIPO_MOTIVO_ANULACION),
                items(ReglasPagos.TIPO_CANAL),
                items(ReglasPagos.TIPO_MEDIO_PAGO),
                items(ReglasPagos.TIPO_MOTIVO_REPORTE),
                items(ReglasPagos.TIPO_ESTADO_REPORTE),
                listaEmpresas);
    }
}
