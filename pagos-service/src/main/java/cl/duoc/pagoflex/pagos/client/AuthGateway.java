package cl.duoc.pagoflex.pagos.client;

import cl.duoc.pagoflex.pagos.dto.PersonaRemotaRequest;
import cl.duoc.pagoflex.pagos.dto.UsuarioRemoto;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthGateway {

    private final AuthClient cliente;
    private final ObjectMapper mapper = new ObjectMapper();

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

    public Optional<UsuarioRemoto> porRut(String rut) {
        try {
            return Optional.ofNullable(cliente.porRut(rut));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }

    public UsuarioRemoto crearPersona(PersonaRemotaRequest peticion) {
        try {
            return cliente.crearPersona(peticion);
        } catch (FeignException.BadRequest | FeignException.Conflict e) {
            throw new NegocioException(HttpStatus.valueOf(e.status()), mensajeDe(e));
        }
    }

    private String mensajeDe(FeignException e) {
        try {
            String mensaje = mapper.readTree(e.contentUTF8()).path("mensaje").asText();
            return mensaje.isEmpty() ? "No pudimos registrar a la persona." : mensaje;
        } catch (Exception ex) {
            return "No pudimos registrar a la persona.";
        }
    }
}
