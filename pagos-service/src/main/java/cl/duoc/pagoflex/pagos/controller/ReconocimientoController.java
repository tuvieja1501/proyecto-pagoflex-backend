package cl.duoc.pagoflex.pagos.controller;

import cl.duoc.pagoflex.pagos.dto.OtorgarReconocimientoRequest;
import cl.duoc.pagoflex.pagos.dto.ReconocimientoDto;
import cl.duoc.pagoflex.pagos.service.ReconocimientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos/reconocimientos")
@Tag(name = "Reconocimientos", description = "Logros obtenidos por las personas")
public class ReconocimientoController {

    private final ReconocimientoService servicio;

    public ReconocimientoController(ReconocimientoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista los logros obtenidos por una persona")
    public List<ReconocimientoDto> listar(@RequestParam("idUsuario") Integer idUsuario) {
        return servicio.listar(idUsuario).stream().map(ReconocimientoDto::desde).toList();
    }

    @PostMapping
    @Operation(summary = "Registra un logro (si ya lo tenía, no se duplica y nunca se pierde)")
    public ResponseEntity<ReconocimientoDto> otorgar(@RequestBody OtorgarReconocimientoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ReconocimientoDto.desde(servicio.otorgar(peticion)));
    }
}
