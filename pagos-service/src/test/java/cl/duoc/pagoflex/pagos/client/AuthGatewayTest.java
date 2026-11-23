package cl.duoc.pagoflex.pagos.client;

import static cl.duoc.pagoflex.pagos.Datos.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cl.duoc.pagoflex.pagos.dto.PersonaRemotaRequest;
import cl.duoc.pagoflex.pagos.dto.UsuarioRemoto;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthGatewayTest {

    private AuthClient cliente;
    private AuthGateway gateway;

    @BeforeEach
    void preparar() {
        cliente = mock(AuthClient.class);
        gateway = new AuthGateway(cliente);
    }

    private PersonaRemotaRequest persona() {
        return new PersonaRemotaRequest("12.345.678-5", "Ana", "Paz", "a@b.cl", "+56912345678");
    }

    @Test
    void usuarioExistenteSeDevuelveEnvuelto() {
        when(cliente.obtener(1)).thenReturn(usuario(1, 1, null));
        assertEquals(1, gateway.usuario(1).orElseThrow().idUsuario());
    }

    @Test
    void usuarioInexistenteDa404YSeTransformaEnVacio() {
        when(cliente.obtener(9)).thenThrow(mock(FeignException.NotFound.class));
        assertTrue(gateway.usuario(9).isEmpty());
    }

    @Test
    void porRutExistenteEInexistente() {
        when(cliente.porRut("12.345.678-5")).thenReturn(usuario(1, 1, null));
        when(cliente.porRut("1.111-1")).thenThrow(mock(FeignException.NotFound.class));
        assertTrue(gateway.porRut("12.345.678-5").isPresent());
        assertTrue(gateway.porRut("1.111-1").isEmpty());
    }

    @Test
    void crearPersonaDevuelveLaPersonaCreada() {
        UsuarioRemoto creada = usuario(40, 1, null);
        when(cliente.crearPersona(persona())).thenReturn(creada);
        assertEquals(40, gateway.crearPersona(persona()).idUsuario());
    }

    @Test
    void crearPersonaTraduceElErrorDelOtroServicio() {
        FeignException.Conflict conflicto = mock(FeignException.Conflict.class);
        when(conflicto.status()).thenReturn(409);
        when(conflicto.contentUTF8()).thenReturn("{\"mensaje\":\"Ya existe una persona con ese RUT.\"}");
        when(cliente.crearPersona(persona())).thenThrow(conflicto);
        NegocioException e = assertThrows(NegocioException.class, () -> gateway.crearPersona(persona()));
        assertEquals(HttpStatus.CONFLICT, e.getEstado());
        assertEquals("Ya existe una persona con ese RUT.", e.getMessage());
    }

    @Test
    void crearPersonaConRespuestaSinMensajeUsaUnTextoGenerico() {
        FeignException.BadRequest malo = mock(FeignException.BadRequest.class);
        when(malo.status()).thenReturn(400);
        when(malo.contentUTF8()).thenReturn("{}");
        when(cliente.crearPersona(persona())).thenThrow(malo);
        assertEquals("No pudimos registrar a la persona.",
                assertThrows(NegocioException.class, () -> gateway.crearPersona(persona())).getMessage());
        FeignException.BadRequest ilegible = mock(FeignException.BadRequest.class);
        when(ilegible.status()).thenReturn(400);
        when(ilegible.contentUTF8()).thenReturn("no es json");
        when(cliente.crearPersona(persona())).thenThrow(ilegible);
        assertEquals("No pudimos registrar a la persona.",
                assertThrows(NegocioException.class, () -> gateway.crearPersona(persona())).getMessage());
    }
}
