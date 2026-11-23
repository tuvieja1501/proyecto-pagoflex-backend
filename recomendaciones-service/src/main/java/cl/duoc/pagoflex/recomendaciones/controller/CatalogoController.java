package cl.duoc.pagoflex.recomendaciones.controller;

import cl.duoc.pagoflex.recomendaciones.dto.CatalogosDto;
import cl.duoc.pagoflex.recomendaciones.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recomendaciones/catalogos")
@Tag(name = "Catálogos", description = "Rubros y estados de recomendación")
public class CatalogoController {

    private final CatalogoService servicio;

    public CatalogoController(CatalogoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Devuelve rubros y estados en una sola llamada")
    public CatalogosDto todos() {
        return servicio.todos();
    }
}
