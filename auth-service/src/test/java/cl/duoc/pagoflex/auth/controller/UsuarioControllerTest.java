package cl.duoc.pagoflex.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.pagoflex.auth.dto.AvisosRequest;
import cl.duoc.pagoflex.auth.dto.ComunaDto;
import cl.duoc.pagoflex.auth.dto.PerfilRequest;
import cl.duoc.pagoflex.auth.dto.PersonaRequest;
import cl.duoc.pagoflex.auth.exception.NegocioException;
import cl.duoc.pagoflex.auth.model.Usuario;
import cl.duoc.pagoflex.auth.service.UsuarioService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService servicio;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MvcSupport.mvc(new UsuarioController(servicio));
    }

    private Usuario usuario(int id) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setIdRol(1);
        u.setRut("12.345.678-5");
        u.setNombres("Camila");
        u.setApellidos("Rojas");
        u.setCorreo("camila@correo.ejemplo");
        u.setActivo(true);
        u.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 9, 0));
        return u;
    }

    @Test
    void obtenerPorId() throws Exception {
        when(servicio.obtener(1)).thenReturn(usuario(1));
        mvc.perform(get("/auth/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Camila"));
    }

    @Test
    void obtenerInexistenteDevuelve404ConMensaje() throws Exception {
        when(servicio.obtener(9)).thenThrow(new NegocioException(HttpStatus.NOT_FOUND, "No encontramos tu cuenta."));
        mvc.perform(get("/auth/usuarios/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos tu cuenta."));
    }

    @Test
    void buscarPorRutConPuntosYGuion() throws Exception {
        when(servicio.porRut("12.345.678-5")).thenReturn(usuario(1));
        mvc.perform(get("/auth/usuarios/rut/12.345.678-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(1));
    }

    @Test
    void loteDeUsuarios() throws Exception {
        when(servicio.lote(any())).thenReturn(List.of(usuario(1), usuario(2)));
        mvc.perform(get("/auth/usuarios").param("ids", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].idUsuario").value(2));
    }

    @Test
    void actualizarPerfil() throws Exception {
        when(servicio.actualizarPerfil(eq(1), any(PerfilRequest.class))).thenReturn(usuario(1));
        mvc.perform(put("/auth/usuarios/1").contentType(MediaType.APPLICATION_JSON)
                        .content(MvcSupport.MAPPER.writeValueAsString(
                                new PerfilRequest("Camila", "Rojas", "c@c.cl", "+56912345678", 1))))
                .andExpect(status().isOk());
    }

    @Test
    void guardarAvisosDevuelve204() throws Exception {
        mvc.perform(put("/auth/usuarios/1/avisos").contentType(MediaType.APPLICATION_JSON)
                        .content(MvcSupport.MAPPER.writeValueAsString(new AvisosRequest(true, 5))))
                .andExpect(status().isNoContent());
        verify(servicio).guardarAvisos(eq(1), any(AvisosRequest.class));
    }

    @Test
    void listarComunas() throws Exception {
        when(servicio.comunas()).thenReturn(List.of(new ComunaDto(1, "Maipú")));
        mvc.perform(get("/auth/comunas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Maipú"));
    }

    @Test
    void crearPersonaDevuelve201() throws Exception {
        when(servicio.crearPersona(any(PersonaRequest.class))).thenReturn(usuario(7));
        mvc.perform(post("/auth/usuarios/personas").contentType(MediaType.APPLICATION_JSON)
                        .content(MvcSupport.MAPPER.writeValueAsString(
                                new PersonaRequest("12.345.678-5", "Camila", "Rojas", "c@c.cl", "+56912345678"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(7));
    }
}
