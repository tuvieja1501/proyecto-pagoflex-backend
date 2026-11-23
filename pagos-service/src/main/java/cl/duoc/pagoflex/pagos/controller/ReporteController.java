package cl.duoc.pagoflex.pagos.controller;

import cl.duoc.pagoflex.pagos.dto.CompromisoDto;
import cl.duoc.pagoflex.pagos.dto.NuevoReporteRequest;
import cl.duoc.pagoflex.pagos.dto.ReporteDto;
import cl.duoc.pagoflex.pagos.dto.ResolverReporteRequest;
import cl.duoc.pagoflex.pagos.dto.TomarReporteRequest;
import cl.duoc.pagoflex.pagos.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
@RequestMapping("/pagos/reportes")
@Tag(name = "Reportes de problema", description = "Reportes de las personas y gestión de los agentes")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista reportes (todos, por usuario o por compromiso)")
    public List<ReporteDto> listar(@RequestParam(name = "idUsuario", required = false) Integer idUsuario,
                                   @RequestParam(name = "idCompromiso", required = false) Integer idCompromiso) {
        return servicio.listar(idUsuario, idCompromiso).stream().map(ReporteDto::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un reporte")
    public ReporteDto obtener(@PathVariable("id") Integer id) {
        return ReporteDto.desde(servicio.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Una persona reporta un problema con un compromiso")
    public ResponseEntity<ReporteDto> crear(@RequestBody NuevoReporteRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ReporteDto.desde(servicio.crear(peticion)));
    }

    @PutMapping("/{id}/toma")
    @Operation(summary = "Un agente toma un reporte abierto")
    public ReporteDto tomar(@PathVariable("id") Integer id, @RequestBody TomarReporteRequest peticion) {
        return ReporteDto.desde(servicio.tomar(id, peticion));
    }

    @PutMapping("/{id}/resolucion")
    @Operation(summary = "El agente resuelve el reporte y se recalcula el estado del compromiso")
    public CompromisoDto resolver(@PathVariable("id") Integer id, @RequestBody ResolverReporteRequest peticion) {
        return CompromisoDto.desde(servicio.resolver(id, peticion));
    }
}
