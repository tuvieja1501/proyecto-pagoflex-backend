package cl.duoc.pagoflex.pagos.controller;

import cl.duoc.pagoflex.pagos.dto.CatalogosDto;
import cl.duoc.pagoflex.pagos.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos/catalogos")
@Tag(name = "Catálogos", description = "Estados, medios de pago, motivos y empresas clientes")
public class CatalogoController {

    private final CatalogoService servicio;

    public CatalogoController(CatalogoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Devuelve todos los catálogos de pagos en una sola llamada")
    public CatalogosDto todos() {
        return servicio.todos();
    }
}
