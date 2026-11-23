package cl.duoc.pagoflex.recomendaciones.client;

import cl.duoc.pagoflex.recomendaciones.dto.ComunaRemota;
import cl.duoc.pagoflex.recomendaciones.dto.UsuarioRemoto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/auth/usuarios/{id}")
    UsuarioRemoto obtener(@PathVariable("id") Integer id);

    @GetMapping("/auth/comunas")
    List<ComunaRemota> comunas();
}
