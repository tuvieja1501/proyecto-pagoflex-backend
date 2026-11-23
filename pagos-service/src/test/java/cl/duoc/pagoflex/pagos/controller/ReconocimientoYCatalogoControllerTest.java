package cl.duoc.pagoflex.pagos.controller;

import static cl.duoc.pagoflex.pagos.Datos.AHORA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.pagoflex.pagos.dto.CatalogosDto;
import cl.duoc.pagoflex.pagos.dto.ItemCatalogoDto;
import cl.duoc.pagoflex.pagos.dto.OtorgarReconocimientoRequest;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import cl.duoc.pagoflex.pagos.model.UsuarioReconocimiento;
import cl.duoc.pagoflex.pagos.service.CatalogoService;
import cl.duoc.pagoflex.pagos.service.ReconocimientoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class ReconocimientoYCatalogoControllerTest {

    @Test
    void listaLosLogrosDeUnaPersona() throws Exception {
        ReconocimientoService servicio = mock(ReconocimientoService.class);
        when(servicio.listar(1)).thenReturn(List.of(new UsuarioReconocimiento(1, 1, 2, AHORA, "Primer pago")));
        MockMvc mvc = MvcSupport.mvc(new ReconocimientoController(servicio));
        mvc.perform(get("/pagos/reconocimientos").param("idUsuario", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idReconocimiento").value(2))
                .andExpect(jsonPath("$[0].motivo").value("Primer pago"));
    }

    @Test
    void sinIdUsuarioLaPeticionEsInvalida() throws Exception {
        MockMvc mvc = MvcSupport.mvc(new ReconocimientoController(mock(ReconocimientoService.class)));
        mvc.perform(get("/pagos/reconocimientos")).andExpect(status().isBadRequest());
    }

    @Test
    void otorgaUnLogroYResponde201() throws Exception {
        ReconocimientoService servicio = mock(ReconocimientoService.class);
        when(servicio.otorgar(any(OtorgarReconocimientoRequest.class)))
                .thenReturn(new UsuarioReconocimiento(1, 1, 2, AHORA, "Primer pago"));
        MockMvc mvc = MvcSupport.mvc(new ReconocimientoController(servicio));
        mvc.perform(post("/pagos/reconocimientos").contentType(MediaType.APPLICATION_JSON)
                        .content(MvcSupport.MAPPER.writeValueAsString(
                                new OtorgarReconocimientoRequest(1, 2, AHORA, "Primer pago"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuarioFinal").value(1));
    }

    @Test
    void otorgarConDatosIncompletosDevuelve400() throws Exception {
        ReconocimientoService servicio = mock(ReconocimientoService.class);
        when(servicio.otorgar(any(OtorgarReconocimientoRequest.class)))
                .thenThrow(new NegocioException(HttpStatus.BAD_REQUEST, "Indica la persona y el reconocimiento."));
        MockMvc mvc = MvcSupport.mvc(new ReconocimientoController(servicio));
        mvc.perform(post("/pagos/reconocimientos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Indica la persona y el reconocimiento."));
    }

    @Test
    void devuelveTodosLosCatalogos() throws Exception {
        CatalogoService servicio = mock(CatalogoService.class);
        List<ItemCatalogoDto> vacio = List.of();
        when(servicio.todos()).thenReturn(new CatalogosDto(
                List.of(new ItemCatalogoDto(1, "Pendiente")), vacio, vacio, List.of(new ItemCatalogoDto(4, "Pago simulado")),
                vacio, vacio, List.of()));
        MockMvc mvc = MvcSupport.mvc(new CatalogoController(servicio));
        mvc.perform(get("/pagos/catalogos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadosCompromiso[0].nombre").value("Pendiente"))
                .andExpect(jsonPath("$.mediosPago[0].id").value(4));
    }
}
