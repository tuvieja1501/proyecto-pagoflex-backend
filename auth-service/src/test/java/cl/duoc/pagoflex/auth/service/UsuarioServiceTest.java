package cl.duoc.pagoflex.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.pagoflex.auth.dto.AvisosRequest;
import cl.duoc.pagoflex.auth.dto.ComunaDto;
import cl.duoc.pagoflex.auth.dto.PerfilRequest;
import cl.duoc.pagoflex.auth.dto.PersonaRequest;
import cl.duoc.pagoflex.auth.exception.NegocioException;
import cl.duoc.pagoflex.auth.model.Comuna;
import cl.duoc.pagoflex.auth.model.Usuario;
import cl.duoc.pagoflex.auth.repository.ComunaRepository;
import cl.duoc.pagoflex.auth.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private ComunaRepository comunas;

    private UsuarioService servicio;
    private Usuario camila;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarios, comunas,
                Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneId.of("UTC")));
        camila = new Usuario();
        camila.setIdUsuario(1);
        camila.setIdRol(1);
        camila.setRut("12.345.678-5");
        camila.setNombres("Camila");
        camila.setApellidos("Rojas");
        camila.setCorreo("camila@correo.ejemplo");
        camila.setActivo(true);
        when(usuarios.findById(1)).thenReturn(Optional.of(camila));
        when(usuarios.findById(2)).thenReturn(Optional.empty());
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(comunas.existsById(1)).thenReturn(true);
        when(comunas.existsById(77)).thenReturn(false);
    }

    private PerfilRequest perfil(String correo, String telefono, Integer comuna) {
        return new PerfilRequest("Camila Andrea", "Rojas Soto", correo, telefono, comuna);
    }

    @Test
    void obtenerExistenteYInexistente() {
        assertEquals("Camila", servicio.obtener(1).getNombres());
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.obtener(2));
        assertEquals(HttpStatus.NOT_FOUND, e.getEstado());
    }

    @Test
    void porRutFormateaAntesDeBuscar() {
        when(usuarios.findByRut("12.345.678-5")).thenReturn(Optional.of(camila));
        assertEquals(1, servicio.porRut("123456785").getIdUsuario());
        when(usuarios.findByRut("1.234-5")).thenReturn(Optional.empty());
        assertThrows(NegocioException.class, () -> servicio.porRut("12345"));
    }

    @Test
    void loteYComunas() {
        when(usuarios.findByIdUsuarioIn(Set.of(1))).thenReturn(List.of(camila));
        assertEquals(1, servicio.lote(Set.of(1)).size());
        when(comunas.findAll()).thenReturn(List.of(new Comuna(1, "Maipú")));
        assertEquals(List.of(new ComunaDto(1, "Maipú")), servicio.comunas());
    }

    @Test
    void actualizaElPerfilDeUnUsuarioFinal() {
        when(usuarios.findByCorreoIgnoreCase("nuevo@correo.ejemplo")).thenReturn(Optional.empty());
        Usuario u = servicio.actualizarPerfil(1, perfil(" nuevo@correo.ejemplo ", " +56912345678 ", 1));
        assertEquals("nuevo@correo.ejemplo", u.getCorreo());
        assertEquals("+56912345678", u.getTelefono());
        assertEquals("Camila Andrea", u.getNombres());
        assertEquals(1, u.getIdComuna());
    }

    @Test
    void elPersonalNoNecesitaComunaNiTelefonoYNoLaCambia() {
        camila.setIdRol(2);
        camila.setIdComuna(5);
        when(usuarios.findByCorreoIgnoreCase("camila@correo.ejemplo")).thenReturn(Optional.of(camila));
        Usuario u = servicio.actualizarPerfil(1, perfil("camila@correo.ejemplo", null, null));
        assertNull(u.getTelefono());
        assertEquals(5, u.getIdComuna());
    }

    @Test
    void rechazaPerfilInvalidoComunaInexistenteOCorreoDeOtro() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(NegocioException.class, () -> servicio.actualizarPerfil(1, perfil("x", "", 1))).getEstado());
        assertEquals("Elige tu comuna.",
                assertThrows(NegocioException.class,
                        () -> servicio.actualizarPerfil(1, perfil("a@b.cl", "+56912345678", 77))).getMessage());
        Usuario otro = new Usuario();
        otro.setIdUsuario(9);
        when(usuarios.findByCorreoIgnoreCase("otro@correo.ejemplo")).thenReturn(Optional.of(otro));
        NegocioException e = assertThrows(NegocioException.class,
                () -> servicio.actualizarPerfil(1, perfil("otro@correo.ejemplo", "+56912345678", 1)));
        assertEquals(HttpStatus.CONFLICT, e.getEstado());
        assertTrue(e.getMessage().contains("otra cuenta"));
    }

    @Test
    void guardaLasPreferenciasDeAvisos() {
        servicio.guardarAvisos(1, new AvisosRequest(false, 7));
        assertEquals(false, camila.getAvisosActivos());
        assertEquals(7, camila.getDiasAnticipacion());
        verify(usuarios).save(camila);
    }

    @Test
    void rechazaDiasDeAvisoFueraDeRango() {
        assertThrows(NegocioException.class, () -> servicio.guardarAvisos(1, new AvisosRequest(true, 0)));
        assertThrows(NegocioException.class, () -> servicio.guardarAvisos(1, new AvisosRequest(true, 31)));
    }

    private PersonaRequest persona(String rut, String correo) {
        return new PersonaRequest(rut, "Benjamín", "Araya", correo, "+56912345678");
    }

    @Test
    void creaUnaPersonaSinClave() {
        when(usuarios.findByRut("11.111.111-1")).thenReturn(Optional.empty());
        when(usuarios.findByCorreoIgnoreCase("b@correo.ejemplo")).thenReturn(Optional.empty());
        Usuario u = servicio.crearPersona(persona("11111111-1", "b@correo.ejemplo"));
        assertEquals("11.111.111-1", u.getRut());
        assertEquals(1, u.getIdRol());
        assertEquals(true, u.getActivo());
        assertEquals(3, u.getDiasAnticipacion());
    }

    @Test
    void rechazaPersonaInvalidaORepetida() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(NegocioException.class, () -> servicio.crearPersona(persona("1-8", "b@c.cl"))).getEstado());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(NegocioException.class, () -> servicio.crearPersona(
                        new PersonaRequest("11.111.111-1", "", "Araya", "b@c.cl", "+56912345678"))).getEstado());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(NegocioException.class, () -> servicio.crearPersona(
                        new PersonaRequest("11.111.111-1", "Benjamín", "", "b@c.cl", "+56912345678"))).getEstado());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(NegocioException.class, () -> servicio.crearPersona(persona("11.111.111-1", "x"))).getEstado());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(NegocioException.class, () -> servicio.crearPersona(
                        new PersonaRequest("11.111.111-1", "Benjamín", "Araya", "b@c.cl", ""))).getEstado());
        when(usuarios.findByRut("11.111.111-1")).thenReturn(Optional.of(camila));
        assertEquals("Ya existe una persona con ese RUT.",
                assertThrows(NegocioException.class, () -> servicio.crearPersona(persona("11.111.111-1", "b@c.cl"))).getMessage());
        when(usuarios.findByRut("11.111.111-1")).thenReturn(Optional.empty());
        when(usuarios.findByCorreoIgnoreCase("b@c.cl")).thenReturn(Optional.of(camila));
        assertEquals("Ya existe una cuenta con ese correo.",
                assertThrows(NegocioException.class, () -> servicio.crearPersona(persona("11.111.111-1", "b@c.cl"))).getMessage());
    }
}
