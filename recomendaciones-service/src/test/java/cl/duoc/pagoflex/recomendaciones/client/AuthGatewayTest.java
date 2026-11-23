package cl.duoc.pagoflex.recomendaciones.client;

import static cl.duoc.pagoflex.recomendaciones.Datos.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cl.duoc.pagoflex.recomendaciones.dto.ComunaRemota;
import feign.FeignException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthGatewayTest {

    private AuthClient cliente;
    private AuthGateway gateway;

    @BeforeEach
    void preparar() {
        cliente = mock(AuthClient.class);
        gateway = new AuthGateway(cliente);
    }

    @Test
    void devuelveElUsuarioExistente() {
        when(cliente.obtener(1)).thenReturn(usuario(1, 1, true));
        assertEquals(1, gateway.usuario(1).orElseThrow().idUsuario());
    }

    @Test
    void unUsuarioInexistenteSeTransformaEnVacio() {
        when(cliente.obtener(9)).thenThrow(mock(FeignException.NotFound.class));
        assertTrue(gateway.usuario(9).isEmpty());
    }

    @Test
    void comprueboSiLaComunaExiste() {
        when(cliente.comunas()).thenReturn(List.of(new ComunaRemota(1, "Maipú"), new ComunaRemota(3, "San Miguel")));
        assertTrue(gateway.existeComuna(3));
        assertFalse(gateway.existeComuna(50));
    }
}
