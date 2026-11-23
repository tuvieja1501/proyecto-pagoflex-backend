package cl.duoc.pagoflex.auth.controller;

import cl.duoc.pagoflex.auth.dto.AvisosRequest;
import cl.duoc.pagoflex.auth.dto.ComunaDto;
import cl.duoc.pagoflex.auth.dto.PerfilRequest;
import cl.duoc.pagoflex.auth.dto.PersonaRequest;
import cl.duoc.pagoflex.auth.dto.UsuarioDto;
import cl.duoc.pagoflex.auth.service.UsuarioService;
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
@RequestMapping("/auth")
@Tag(name = "Usuarios", description = "Perfil, preferencias y consultas entre servicios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/usuarios/{id}")
    @Operation(summary = "Obtiene un usuario por id")
    public UsuarioDto obtener(@PathVariable("id") Integer id) {
        return UsuarioDto.desde(servicio.obtener(id));
    }

    @GetMapping("/usuarios/rut/{rut}")
    @Operation(summary = "Busca un usuario por RUT")
    public UsuarioDto porRut(@PathVariable("rut") String rut) {
        return UsuarioDto.desde(servicio.porRut(rut));
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Obtiene varios usuarios por id (uso entre servicios)")
    public List<UsuarioDto> lote(@RequestParam("ids") List<Integer> ids) {
        return servicio.lote(ids).stream().map(UsuarioDto::desde).toList();
    }

    @PutMapping("/usuarios/{id}")
    @Operation(summary = "Modifica el perfil")
    public UsuarioDto actualizar(@PathVariable("id") Integer id, @RequestBody PerfilRequest peticion) {
        return UsuarioDto.desde(servicio.actualizarPerfil(id, peticion));
    }

    @PutMapping("/usuarios/{id}/avisos")
    @Operation(summary = "Guarda las preferencias de avisos de vencimiento")
    public ResponseEntity<Void> avisos(@PathVariable("id") Integer id, @RequestBody AvisosRequest peticion) {
        servicio.guardarAvisos(id, peticion);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/usuarios/personas")
    @Operation(summary = "Registra a una persona sin clave (la crea una empresa cliente al cargar un compromiso)")
    public ResponseEntity<UsuarioDto> crearPersona(@RequestBody PersonaRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDto.desde(servicio.crearPersona(peticion)));
    }

    @GetMapping("/comunas")
    @Operation(summary = "Lista las comunas")
    public List<ComunaDto> comunas() {
        return servicio.comunas();
    }
}
