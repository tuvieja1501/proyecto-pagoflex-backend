package cl.duoc.pagoflex.recomendaciones.client;

import cl.duoc.pagoflex.recomendaciones.dto.UsuarioRemoto;
import feign.FeignException;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AuthGateway {

    private final AuthClient cliente;

    public AuthGateway(AuthClient cliente) {
        this.cliente = cliente;
    }

    public Optional<UsuarioRemoto> usuario(Integer id) {
        try {
            return Optional.ofNullable(cliente.obtener(id));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }

    public boolean existeComuna(Integer idComuna) {
        return cliente.comunas().stream().anyMatch(c -> c.idComuna().equals(idComuna));
    }
}
