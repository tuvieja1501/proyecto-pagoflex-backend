package cl.duoc.pagoflex.recomendaciones.controller;

import cl.duoc.pagoflex.recomendaciones.dto.CambiarEstadoRequest;
import cl.duoc.pagoflex.recomendaciones.dto.NuevaRecomendacionRequest;
import cl.duoc.pagoflex.recomendaciones.dto.RecomendacionDto;
import cl.duoc.pagoflex.recomendaciones.dto.ResumenDto;
import cl.duoc.pagoflex.recomendaciones.service.RecomendacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recomendaciones")
@Tag(name = "Recomendaciones", description = "Organizaciones recomendadas por las personas y su seguimiento")
public class RecomendacionController {

    private final RecomendacionService servicio;

    public RecomendacionController(RecomendacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista recomendaciones (todas o de una persona)")
    public List<RecomendacionDto> listar(@RequestParam(name = "idUsuario", required = false) Integer idUsuario) {
        return servicio.listar(idUsuario);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una recomendación con su contacto autorizado e historial")
    public RecomendacionDto obtener(@PathVariable("id") Integer id) {
        return servicio.obtener(id);
    }

    @GetMapping("/resumen")
    @Operation(summary = "Recomendaciones enviadas en el mes y organizaciones que se unieron")
    public ResumenDto resumen(@RequestParam("idUsuario") Integer idUsuario,
                              @RequestParam(name = "hoy", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hoy) {
        return servicio.resumen(idUsuario, hoy);
    }

    @PostMapping
    @Operation(summary = "Una persona recomienda una organización (máximo 5 por mes, sin duplicados)")
    public ResponseEntity<RecomendacionDto> crear(@RequestBody NuevaRecomendacionRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(peticion));
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Un agente avanza el estado de la recomendación sin saltar etapas")
    public RecomendacionDto cambiarEstado(@PathVariable("id") Integer id,
                                          @RequestBody CambiarEstadoRequest peticion) {
        return servicio.cambiarEstado(id, peticion);
    }
}
