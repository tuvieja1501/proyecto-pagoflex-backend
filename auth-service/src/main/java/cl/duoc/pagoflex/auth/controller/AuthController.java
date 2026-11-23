package cl.duoc.pagoflex.auth.controller;

import cl.duoc.pagoflex.auth.dto.CambioClaveRequest;
import cl.duoc.pagoflex.auth.dto.CodigoRecuperacionDto;
import cl.duoc.pagoflex.auth.dto.LoginRequest;
import cl.duoc.pagoflex.auth.dto.RecuperacionRequest;
import cl.duoc.pagoflex.auth.dto.RegistroRequest;
import cl.duoc.pagoflex.auth.dto.RestablecerRequest;
import cl.duoc.pagoflex.auth.dto.UsuarioDto;
import cl.duoc.pagoflex.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, registro y recuperación de clave")
public class AuthController {

    private final AuthService servicio;

    public AuthController(AuthService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión con correo o RUT y clave")
    public UsuarioDto login(@RequestBody LoginRequest peticion) {
        return UsuarioDto.desde(servicio.iniciarSesion(peticion));
    }

    @PostMapping("/registro")
    @Operation(summary = "Crea una cuenta de usuario final o activa la ya registrada por una empresa")
    public ResponseEntity<UsuarioDto> registro(@RequestBody RegistroRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDto.desde(servicio.registrar(peticion)));
    }

    @PostMapping("/recuperacion")
    @Operation(summary = "Genera un código de recuperación de 6 dígitos (válido 10 minutos)")
    public CodigoRecuperacionDto recuperacion(@RequestBody RecuperacionRequest peticion) {
        return servicio.solicitarRecuperacion(peticion.correo());
    }

    @PostMapping("/restablecer")
    @Operation(summary = "Cambia la clave usando el código de recuperación")
    public ResponseEntity<Void> restablecer(@RequestBody RestablecerRequest peticion) {
        servicio.restablecerClave(peticion);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/usuarios/{id}/clave")
    @Operation(summary = "Cambia la clave pidiendo la actual")
    public ResponseEntity<Void> cambiarClave(@PathVariable("id") Integer id, @RequestBody CambioClaveRequest peticion) {
        servicio.cambiarClave(id, peticion);
        return ResponseEntity.noContent().build();
    }
}
