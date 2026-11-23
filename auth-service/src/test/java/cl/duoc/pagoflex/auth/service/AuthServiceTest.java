package cl.duoc.pagoflex.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.pagoflex.auth.dto.CambioClaveRequest;
import cl.duoc.pagoflex.auth.dto.CodigoRecuperacionDto;
import cl.duoc.pagoflex.auth.dto.LoginRequest;
import cl.duoc.pagoflex.auth.dto.RegistroRequest;
import cl.duoc.pagoflex.auth.dto.RestablecerRequest;
import cl.duoc.pagoflex.auth.exception.NegocioException;
import cl.duoc.pagoflex.auth.model.Credencial;
import cl.duoc.pagoflex.auth.model.Usuario;
import cl.duoc.pagoflex.auth.repository.ComunaRepository;
import cl.duoc.pagoflex.auth.repository.CredencialRepository;
import cl.duoc.pagoflex.auth.repository.UsuarioRepository;
import cl.duoc.pagoflex.auth.security.PasswordHasher;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
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
class AuthServiceTest {

    private static final String CLAVE = "PagoFlex2026";

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private CredencialRepository credenciales;
    @Mock
    private ComunaRepository comunas;

    private final PasswordHasher hasher = new PasswordHasher();
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneId.of("UTC"));
    private AuthService servicio;
    private Usuario camila;
    private Credencial credencial;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(usuarios, credenciales, comunas, hasher, reloj);
        camila = usuario(1, "12.345.678-5", "camila@correo.ejemplo", 1);
        credencial = credencial(1, CLAVE);
        when(usuarios.findByCorreoIgnoreCase("camila@correo.ejemplo")).thenReturn(Optional.of(camila));
        when(usuarios.findByRut("12.345.678-5")).thenReturn(Optional.of(camila));
        when(credenciales.findById(1)).thenReturn(Optional.of(credencial));
        when(comunas.existsById(1)).thenReturn(true);
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            if (u.getIdUsuario() == null) {
                u.setIdUsuario(99);
            }
            return u;
        });
    }

    private Usuario usuario(int id, String rut, String correo, int rol) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setIdRol(rol);
        u.setRut(rut);
        u.setNombres("Camila Andrea");
        u.setApellidos("Rojas Soto");
        u.setCorreo(correo);
        u.setActivo(true);
        u.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 9, 0));
        return u;
    }

    private Credencial credencial(int id, String clave) {
        Credencial c = new Credencial();
        c.setIdUsuario(id);
        c.setSal(hasher.nuevaSal());
        c.setClaveHash(hasher.hash(clave, c.getSal()));
        c.setIntentosFallidos(0);
        c.setFechaCambio(LocalDateTime.of(2026, 1, 1, 9, 0));
        return c;
    }

    private NegocioException falla(Runnable accion) {
        return assertThrows(NegocioException.class, accion::run);
    }

    @Test
    void loginCorrectoPorCorreoDevuelveElUsuario() {
        Usuario u = servicio.iniciarSesion(new LoginRequest("camila@correo.ejemplo", CLAVE));
        assertEquals(1, u.getIdUsuario());
    }

    @Test
    void loginCorrectoPorRutSinFormatoTambienFunciona() {
        Usuario u = servicio.iniciarSesion(new LoginRequest("123456785", CLAVE));
        assertEquals("camila@correo.ejemplo", u.getCorreo());
    }

    @Test
    void loginLimpiaLosIntentosFallidosAlEntrar() {
        credencial.setIntentosFallidos(3);
        servicio.iniciarSesion(new LoginRequest("camila@correo.ejemplo", CLAVE));
        assertEquals(0, credencial.getIntentosFallidos());
        verify(credenciales).save(credencial);
    }

    @Test
    void loginConIdentificadorInvalidoDa400() {
        NegocioException e = falla(() -> servicio.iniciarSesion(new LoginRequest("hola", CLAVE)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
    }

    @Test
    void loginSinClaveDa400() {
        NegocioException e = falla(() -> servicio.iniciarSesion(new LoginRequest("camila@correo.ejemplo", "")));
        assertEquals("Escribe tu clave.", e.getMessage());
        e = falla(() -> servicio.iniciarSesion(new LoginRequest("camila@correo.ejemplo", null)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
    }

    @Test
    void loginConUsuarioInexistenteDa401Generico() {
        when(usuarios.findByCorreoIgnoreCase("nadie@correo.ejemplo")).thenReturn(Optional.empty());
        NegocioException e = falla(() -> servicio.iniciarSesion(new LoginRequest("nadie@correo.ejemplo", CLAVE)));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getEstado());
    }

    @Test
    void loginDeCuentaSinClaveDa409() {
        when(credenciales.findById(1)).thenReturn(Optional.empty());
        NegocioException e = falla(() -> servicio.iniciarSesion(new LoginRequest("camila@correo.ejemplo", CLAVE)));
        assertEquals(HttpStatus.CONFLICT, e.getEstado());
    }

    @Test
    void claveIncorrectaSumaIntentoYAvisaCuantosQuedan() {
        NegocioException e = falla(() -> servicio.iniciarSesion(new LoginRequest("camila@correo.ejemplo", "Otra12345")));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getEstado());
        assertTrue(e.getMessage().endsWith("Te quedan 4 intento(s)."));
        assertEquals(1, credencial.getIntentosFallidos());
        verify(credenciales).save(credencial);
    }

    @Test
    void alQuintoIntentoFallidoSeBloqueaPorCincoMinutos() {
        credencial.setIntentosFallidos(4);
        NegocioException e = falla(() -> servicio.iniciarSesion(new LoginRequest("camila@correo.ejemplo", "Otra12345")));
        assertEquals(HttpStatus.LOCKED, e.getEstado());
        assertEquals(LocalDateTime.of(2026, 10, 6, 12, 5), credencial.getBloqueadoHasta());
        assertEquals(0, credencial.getIntentosFallidos());
    }

    @Test
    void conBloqueoVigenteNiSiquieraRevisaLaClave() {
        credencial.setBloqueadoHasta(LocalDateTime.of(2026, 10, 6, 12, 3));
        NegocioException e = falla(() -> servicio.iniciarSesion(new LoginRequest("camila@correo.ejemplo", CLAVE)));
        assertEquals(HttpStatus.LOCKED, e.getEstado());
        assertTrue(e.getMessage().contains("12:03"));
    }

    @Test
    void conBloqueoVencidoSePuedeEntrarYSeLimpia() {
        credencial.setBloqueadoHasta(LocalDateTime.of(2026, 10, 6, 11, 0));
        servicio.iniciarSesion(new LoginRequest("camila@correo.ejemplo", CLAVE));
        assertNull(credencial.getBloqueadoHasta());
    }

    @Test
    void cuentaInactivaNoEntra() {
        camila.setActivo(false);
        NegocioException e = falla(() -> servicio.iniciarSesion(new LoginRequest("camila@correo.ejemplo", CLAVE)));
        assertEquals(HttpStatus.FORBIDDEN, e.getEstado());
    }

    private RegistroRequest registro(String rut, String correo, String clave) {
        return new RegistroRequest(rut, "Benjamín", "Araya", correo, "+56912345678", 1, clave);
    }

    @Test
    void registroNuevoCreaUsuarioYCredencial() {
        when(usuarios.findByRut("11.111.111-1")).thenReturn(Optional.empty());
        when(usuarios.findByCorreoIgnoreCase("nuevo@correo.ejemplo")).thenReturn(Optional.empty());
        Usuario u = servicio.registrar(registro("11.111.111-1", "nuevo@correo.ejemplo", CLAVE));
        assertEquals(99, u.getIdUsuario());
        assertEquals(AuthService.ROL_USUARIO_FINAL, u.getIdRol());
        assertEquals("11.111.111-1", u.getRut());
        assertTrue(u.getAvisosActivos());
        assertEquals(3, u.getDiasAnticipacion());
        verify(credenciales).save(any(Credencial.class));
    }

    @Test
    void registroInvalidoDa400SinGuardarNada() {
        NegocioException e = falla(() -> servicio.registrar(registro("1-8", "nuevo@correo.ejemplo", CLAVE)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void registroConComunaInexistenteDa400() {
        when(comunas.existsById(1)).thenReturn(false);
        NegocioException e = falla(() -> servicio.registrar(registro("11.111.111-1", "nuevo@correo.ejemplo", CLAVE)));
        assertEquals("Elige tu comuna.", e.getMessage());
    }

    @Test
    void registroConCorreoRepetidoDa409() {
        when(usuarios.findByRut("11.111.111-1")).thenReturn(Optional.empty());
        NegocioException e = falla(() -> servicio.registrar(registro("11.111.111-1", "camila@correo.ejemplo", CLAVE)));
        assertEquals(HttpStatus.CONFLICT, e.getEstado());
        assertEquals("Ya existe una cuenta con ese correo.", e.getMessage());
    }

    @Test
    void registroDeRutDelPersonalDa409() {
        camila.setIdRol(2);
        NegocioException e = falla(() -> servicio.registrar(registro("12.345.678-5", "camila@correo.ejemplo", CLAVE)));
        assertTrue(e.getMessage().contains("personal de PagoFlex"));
    }

    @Test
    void registroDeRutConCuentaYaActivaDa409() {
        when(credenciales.existsById(1)).thenReturn(true);
        NegocioException e = falla(() -> servicio.registrar(registro("12.345.678-5", "camila@correo.ejemplo", CLAVE)));
        assertTrue(e.getMessage().startsWith("Ya existe una cuenta con este RUT"));
    }

    @Test
    void registroDePersonaCargadaPorEmpresaExigeElMismoCorreo() {
        when(credenciales.existsById(1)).thenReturn(false);
        NegocioException e = falla(() -> servicio.registrar(registro("12.345.678-5", "otro@correo.ejemplo", CLAVE)));
        assertTrue(e.getMessage().startsWith("Una empresa ya te registró"));
    }

    @Test
    void registroDePersonaCargadaPorEmpresaActivaLaCuenta() {
        when(credenciales.existsById(1)).thenReturn(false);
        Usuario u = servicio.registrar(registro("12.345.678-5", "CAMILA@correo.ejemplo", CLAVE));
        assertEquals(1, u.getIdUsuario());
        assertEquals("+56912345678", u.getTelefono());
        verify(credenciales).save(any(Credencial.class));
    }

    @Test
    void recuperacionGeneraCodigoConExpiracionDeDiezMinutos() {
        CodigoRecuperacionDto dto = servicio.solicitarRecuperacion("camila@correo.ejemplo");
        assertEquals("Camila", dto.nombre());
        assertTrue(dto.codigo().matches("\\d{6}"));
        assertEquals(LocalDateTime.of(2026, 10, 6, 12, 10), dto.expira());
        assertNotNull(credencial.getCodigoHash());
        assertFalse(credencial.getCodigoHash().contains(dto.codigo()));
    }

    @Test
    void recuperacionConCorreoInvalidoOInexistenteOSinClave() {
        assertEquals(HttpStatus.BAD_REQUEST, falla(() -> servicio.solicitarRecuperacion("x")).getEstado());
        when(usuarios.findByCorreoIgnoreCase("nadie@correo.ejemplo")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, falla(() -> servicio.solicitarRecuperacion("nadie@correo.ejemplo")).getEstado());
        when(credenciales.findById(1)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.CONFLICT, falla(() -> servicio.solicitarRecuperacion("camila@correo.ejemplo")).getEstado());
    }

    private String pedirCodigo() {
        return servicio.solicitarRecuperacion("camila@correo.ejemplo").codigo();
    }

    @Test
    void restablecerConCodigoCorrectoCambiaLaClave() {
        String codigo = pedirCodigo();
        servicio.restablecerClave(new RestablecerRequest("camila@correo.ejemplo", codigo, "Nueva12345"));
        verify(credenciales).save(org.mockito.ArgumentMatchers.argThat(c ->
                hasher.verificar("Nueva12345", c.getSal(), c.getClaveHash())));
    }

    @Test
    void restablecerValidaCodigoYClaveNueva() {
        assertEquals(HttpStatus.BAD_REQUEST,
                falla(() -> servicio.restablecerClave(new RestablecerRequest("camila@correo.ejemplo", "12", "Nueva12345"))).getEstado());
        assertEquals(HttpStatus.BAD_REQUEST,
                falla(() -> servicio.restablecerClave(new RestablecerRequest("camila@correo.ejemplo", "123456", "corta"))).getEstado());
    }

    @Test
    void restablecerConCorreoNuloOInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND,
                falla(() -> servicio.restablecerClave(new RestablecerRequest(null, "123456", "Nueva12345"))).getEstado());
        when(usuarios.findByCorreoIgnoreCase("nadie@correo.ejemplo")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                falla(() -> servicio.restablecerClave(new RestablecerRequest("nadie@correo.ejemplo", "123456", "Nueva12345"))).getEstado());
    }

    @Test
    void restablecerSinCredencialOSinCodigoPedidoDa409() {
        assertEquals(HttpStatus.CONFLICT,
                falla(() -> servicio.restablecerClave(new RestablecerRequest("camila@correo.ejemplo", "123456", "Nueva12345"))).getEstado());
        when(credenciales.findById(1)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.CONFLICT,
                falla(() -> servicio.restablecerClave(new RestablecerRequest("camila@correo.ejemplo", "123456", "Nueva12345"))).getEstado());
    }

    @Test
    void restablecerConCodigoVencidoDa410() {
        credencial.setCodigoHash("abc");
        credencial.setCodigoExpira(LocalDateTime.of(2026, 10, 6, 11, 0));
        assertEquals(HttpStatus.GONE,
                falla(() -> servicio.restablecerClave(new RestablecerRequest("camila@correo.ejemplo", "123456", "Nueva12345"))).getEstado());
        credencial.setCodigoExpira(null);
        assertEquals(HttpStatus.GONE,
                falla(() -> servicio.restablecerClave(new RestablecerRequest("camila@correo.ejemplo", "123456", "Nueva12345"))).getEstado());
    }

    @Test
    void codigoIncorrectoSumaIntentosYAlQuintoSeAnula() {
        String codigo = pedirCodigo();
        String malo = codigo.equals("000000") ? "111111" : "000000";
        NegocioException e = falla(() -> servicio.restablecerClave(new RestablecerRequest("camila@correo.ejemplo", malo, "Nueva12345")));
        assertTrue(e.getMessage().endsWith("Te quedan 4 intento(s)."));
        credencial.setIntentosFallidos(4);
        e = falla(() -> servicio.restablecerClave(new RestablecerRequest("camila@correo.ejemplo", malo, "Nueva12345")));
        assertEquals("Por seguridad anulamos el código. Pide uno nuevo.", e.getMessage());
        assertNull(credencial.getCodigoHash());
    }

    @Test
    void cambiarClaveConLaActualCorrecta() {
        servicio.cambiarClave(1, new CambioClaveRequest(CLAVE, "Nueva12345"));
        verify(credenciales).save(org.mockito.ArgumentMatchers.argThat(c ->
                hasher.verificar("Nueva12345", c.getSal(), c.getClaveHash())));
    }

    @Test
    void cambiarClaveRechazaActualIncorrectaInvalidaOSinCredencial() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                falla(() -> servicio.cambiarClave(1, new CambioClaveRequest("Mala12345", "Nueva12345"))).getEstado());
        assertEquals(HttpStatus.BAD_REQUEST,
                falla(() -> servicio.cambiarClave(1, new CambioClaveRequest(CLAVE, CLAVE))).getEstado());
        when(credenciales.findById(anyInt())).thenReturn(Optional.empty());
        assertEquals(HttpStatus.CONFLICT,
                falla(() -> servicio.cambiarClave(5, new CambioClaveRequest(CLAVE, "Nueva12345"))).getEstado());
    }
}
