package cl.duoc.pagoflex.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.pagoflex.auth.dto.CambioClaveRequest;
import cl.duoc.pagoflex.auth.dto.CodigoRecuperacionDto;
import cl.duoc.pagoflex.auth.dto.LoginRequest;
import cl.duoc.pagoflex.auth.dto.RecuperacionRequest;
import cl.duoc.pagoflex.auth.dto.RegistroRequest;
import cl.duoc.pagoflex.auth.dto.RestablecerRequest;
import cl.duoc.pagoflex.auth.exception.NegocioException;
import cl.duoc.pagoflex.auth.model.Usuario;
import cl.duoc.pagoflex.auth.service.AuthService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService servicio;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MvcSupport.mvc(new AuthController(servicio));
    }

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setIdRol(1);
        u.setRut("12.345.678-5");
        u.setNombres("Camila");
        u.setApellidos("Rojas");
        u.setCorreo("camila@correo.ejemplo");
        u.setActivo(true);
        u.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 9, 0));
        return u;
    }

    private String json(Object o) throws Exception {
        return MvcSupport.MAPPER.writeValueAsString(o);
    }

    @Test
    void loginDevuelveElUsuario() throws Exception {
        when(servicio.iniciarSesion(any(LoginRequest.class))).thenReturn(usuario());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("camila@correo.ejemplo", "PagoFlex2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.correo").value("camila@correo.ejemplo"))
                .andExpect(jsonPath("$.fechaCreacion").value("2026-01-01T09:00:00"));
    }

    @Test
    void loginFallidoDevuelveElMensajeDelError() throws Exception {
        when(servicio.iniciarSesion(any(LoginRequest.class)))
                .thenThrow(new NegocioException(HttpStatus.UNAUTHORIZED, "No coinciden"));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("camila@correo.ejemplo", "mala"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("No coinciden"));
    }

    @Test
    void registroDevuelve201() throws Exception {
        when(servicio.registrar(any(RegistroRequest.class))).thenReturn(usuario());
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegistroRequest("12.345.678-5", "Camila", "Rojas", "c@c.cl", "+56912345678", 1, "PagoFlex2026"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rut").value("12.345.678-5"));
    }

    @Test
    void recuperacionDevuelveElCodigo() throws Exception {
        when(servicio.solicitarRecuperacion("camila@correo.ejemplo"))
                .thenReturn(new CodigoRecuperacionDto("Camila", "123456", LocalDateTime.of(2026, 10, 6, 12, 10)));
        mvc.perform(post("/auth/recuperacion").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RecuperacionRequest("camila@correo.ejemplo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("123456"))
                .andExpect(jsonPath("$.nombre").value("Camila"));
    }

    @Test
    void restablecerDevuelve204() throws Exception {
        mvc.perform(post("/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RestablecerRequest("camila@correo.ejemplo", "123456", "Nueva12345"))))
                .andExpect(status().isNoContent());
        verify(servicio).restablecerClave(any(RestablecerRequest.class));
    }

    @Test
    void restablecerConCodigoVencidoDevuelve410() throws Exception {
        doThrow(new NegocioException(HttpStatus.GONE, "El código venció. Pide uno nuevo."))
                .when(servicio).restablecerClave(any(RestablecerRequest.class));
        mvc.perform(post("/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RestablecerRequest("camila@correo.ejemplo", "123456", "Nueva12345"))))
                .andExpect(status().isGone());
    }

    @Test
    void cambiarClaveDevuelve204() throws Exception {
        mvc.perform(put("/auth/usuarios/1/clave").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CambioClaveRequest("PagoFlex2026", "Nueva12345"))))
                .andExpect(status().isNoContent());
        verify(servicio).cambiarClave(eq(1), any(CambioClaveRequest.class));
    }
}
