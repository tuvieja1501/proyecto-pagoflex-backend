package cl.duoc.pagoflex.pagos.client;

import cl.duoc.pagoflex.pagos.dto.PersonaRemotaRequest;
import cl.duoc.pagoflex.pagos.dto.UsuarioRemoto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/auth/usuarios/{id}")
    UsuarioRemoto obtener(@PathVariable("id") Integer id);

    @GetMapping("/auth/usuarios/rut/{rut}")
    UsuarioRemoto porRut(@PathVariable("rut") String rut);

    @PostMapping("/auth/usuarios/personas")
    UsuarioRemoto crearPersona(@RequestBody PersonaRemotaRequest peticion);
}
