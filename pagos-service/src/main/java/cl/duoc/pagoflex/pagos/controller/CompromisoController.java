package cl.duoc.pagoflex.pagos.controller;

import cl.duoc.pagoflex.pagos.dto.AnulacionDto;
import cl.duoc.pagoflex.pagos.dto.AnularRequest;
import cl.duoc.pagoflex.pagos.dto.CompromisoDto;
import cl.duoc.pagoflex.pagos.dto.PagarRequest;
import cl.duoc.pagoflex.pagos.dto.PagoDto;
import cl.duoc.pagoflex.pagos.dto.RegistrarCompromisoRequest;
import cl.duoc.pagoflex.pagos.dto.VencimientosRequest;
import cl.duoc.pagoflex.pagos.service.CompromisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/pagos")
@Tag(name = "Compromisos y pagos", description = "Cuotas, pagos, comprobantes y anulaciones")
public class CompromisoController {

    private final CompromisoService servicio;

    public CompromisoController(CompromisoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/compromisos")
    @Operation(summary = "Lista compromisos por usuario, por empresa o por ids")
    public List<CompromisoDto> listar(@RequestParam(name = "idUsuario", required = false) Integer idUsuario,
                                      @RequestParam(name = "idEmpresa", required = false) Integer idEmpresa,
                                      @RequestParam(name = "ids", required = false) List<Integer> ids) {
        return servicio.listar(idUsuario, idEmpresa, ids).stream().map(CompromisoDto::desde).toList();
    }

    @GetMapping("/compromisos/{id}")
    @Operation(summary = "Obtiene un compromiso")
    public CompromisoDto obtener(@PathVariable("id") Integer id) {
        return CompromisoDto.desde(servicio.obtener(id));
    }

    @PostMapping("/compromisos")
    @Operation(summary = "Un ejecutivo registra un compromiso (crea a la persona si no existe)")
    public ResponseEntity<CompromisoDto> registrar(@RequestBody RegistrarCompromisoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CompromisoDto.desde(servicio.registrar(peticion)));
    }

    @PostMapping("/compromisos/{id}/pago")
    @Operation(summary = "Paga un compromiso y emite el comprobante")
    public ResponseEntity<PagoDto> pagar(@PathVariable("id") Integer id, @RequestBody PagarRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PagoDto.desde(servicio.pagar(id, peticion)));
    }

    @PutMapping("/compromisos/{id}/anulacion")
    @Operation(summary = "Un ejecutivo anula un compromiso de su empresa")
    public AnulacionDto anular(@PathVariable("id") Integer id, @RequestBody AnularRequest peticion) {
        return AnulacionDto.desde(servicio.anular(id, peticion));
    }

    @GetMapping("/pagos")
    @Operation(summary = "Lista pagos por usuario, empresa o compromiso")
    public List<PagoDto> pagos(@RequestParam(name = "idUsuario", required = false) Integer idUsuario,
                               @RequestParam(name = "idEmpresa", required = false) Integer idEmpresa,
                               @RequestParam(name = "idCompromiso", required = false) Integer idCompromiso) {
        return servicio.pagosDe(idUsuario, idEmpresa, idCompromiso).stream().map(PagoDto::desde).toList();
    }

    @GetMapping("/anulaciones")
    @Operation(summary = "Lista anulaciones por usuario, empresa o compromiso")
    public List<AnulacionDto> anulaciones(@RequestParam(name = "idUsuario", required = false) Integer idUsuario,
                                          @RequestParam(name = "idEmpresa", required = false) Integer idEmpresa,
                                          @RequestParam(name = "idCompromiso", required = false) Integer idCompromiso) {
        return servicio.anulacionesDe(idUsuario, idEmpresa, idCompromiso).stream().map(AnulacionDto::desde).toList();
    }

    @PostMapping("/vencimientos")
    @Operation(summary = "Marca como vencidos los compromisos pendientes que ya pasaron su fecha y aplica el recargo")
    public Map<String, Integer> vencimientos(@RequestBody VencimientosRequest peticion) {
        return Map.of("actualizados", servicio.actualizarVencimientos(peticion));
    }
}
