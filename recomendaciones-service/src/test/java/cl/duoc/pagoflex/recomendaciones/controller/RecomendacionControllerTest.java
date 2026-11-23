package cl.duoc.pagoflex.recomendaciones.controller;

import static cl.duoc.pagoflex.recomendaciones.Datos.AHORA;
import static cl.duoc.pagoflex.recomendaciones.Datos.HOY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.pagoflex.recomendaciones.dto.CambiarEstadoRequest;
import cl.duoc.pagoflex.recomendaciones.dto.CambioDto;
import cl.duoc.pagoflex.recomendaciones.dto.CatalogosDto;
import cl.duoc.pagoflex.recomendaciones.dto.ContactoDto;
import cl.duoc.pagoflex.recomendaciones.dto.ItemCatalogoDto;
import cl.duoc.pagoflex.recomendaciones.dto.NuevaRecomendacionRequest;
import cl.duoc.pagoflex.recomendaciones.dto.RecomendacionDto;
import cl.duoc.pagoflex.recomendaciones.dto.ResumenDto;
import cl.duoc.pagoflex.recomendaciones.exception.NegocioException;
import cl.duoc.pagoflex.recomendaciones.service.CatalogoService;
import cl.duoc.pagoflex.recomendaciones.service.RecomendacionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class RecomendacionControllerTest {

    private RecomendacionService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(RecomendacionService.class);
        mvc = MvcSupport.mvc(new RecomendacionController(servicio));
    }

    private RecomendacionDto dto(int id) {
        return new RecomendacionDto(id, "REC-000" + id, 1, "Gimnasio Fuerza Sur", null, 11, 3, AHORA, 2,
                new ContactoDto("Raúl", null, "+56912345678"),
                List.of(new CambioDto(2, 9, AHORA, null)));
    }

    private String json(Object o) throws Exception {
        return MvcSupport.MAPPER.writeValueAsString(o);
    }

    @Test
    void listaRecomendacionesDeUnaPersona() throws Exception {
        when(servicio.listar(1)).thenReturn(List.of(dto(1)));
        mvc.perform(get("/recomendaciones").param("idUsuario", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("REC-0001"))
                .andExpect(jsonPath("$[0].contacto.nombre").value("Raúl"))
                .andExpect(jsonPath("$[0].cambios[0].idAgente").value(9));
    }

    @Test
    void listaTodasLasRecomendaciones() throws Exception {
        when(servicio.listar(isNull())).thenReturn(List.of(dto(1), dto(2)));
        mvc.perform(get("/recomendaciones")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void obtieneUnaRecomendacionOResponde404() throws Exception {
        when(servicio.obtener(1)).thenReturn(dto(1));
        when(servicio.obtener(99)).thenThrow(new NegocioException(HttpStatus.NOT_FOUND, "No encontramos la recomendación."));
        mvc.perform(get("/recomendaciones/1")).andExpect(status().isOk());
        mvc.perform(get("/recomendaciones/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos la recomendación."));
    }

    @Test
    void devuelveElResumenDelMes() throws Exception {
        when(servicio.resumen(1, HOY)).thenReturn(new ResumenDto(3, 1));
        when(servicio.resumen(1, null)).thenReturn(new ResumenDto(0, 0));
        mvc.perform(get("/recomendaciones/resumen").param("idUsuario", "1").param("hoy", "2026-10-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enviadasEsteMes").value(3))
                .andExpect(jsonPath("$.seUnieron").value(1));
        mvc.perform(get("/recomendaciones/resumen").param("idUsuario", "1"))
                .andExpect(jsonPath("$.enviadasEsteMes").value(0));
    }

    @Test
    void creaUnaRecomendacionYResponde201() throws Exception {
        when(servicio.crear(any(NuevaRecomendacionRequest.class))).thenReturn(dto(5));
        NuevaRecomendacionRequest p = new NuevaRecomendacionRequest(1, "Gimnasio Fuerza Sur", null, 11, 3, null, null,
                null, null, AHORA);
        mvc.perform(post("/recomendaciones").contentType(MediaType.APPLICATION_JSON).content(json(p)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRecomendacion").value(5));
    }

    @Test
    void crearDuplicadaDevuelve409ConElMensaje() throws Exception {
        when(servicio.crear(any(NuevaRecomendacionRequest.class)))
                .thenThrow(new NegocioException(HttpStatus.CONFLICT, "Ya recomendaste a «Club» (REC-0003)."));
        mvc.perform(post("/recomendaciones").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya recomendaste a «Club» (REC-0003)."));
    }

    @Test
    void cambiaElEstado() throws Exception {
        when(servicio.cambiarEstado(eq(1), any(CambiarEstadoRequest.class))).thenReturn(dto(1));
        mvc.perform(put("/recomendaciones/1/estado").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CambiarEstadoRequest(9, 3, null, AHORA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRecomendacion").value(1));
    }

    @Test
    void saltarseEtapasDevuelve409() throws Exception {
        when(servicio.cambiarEstado(eq(1), any(CambiarEstadoRequest.class)))
                .thenThrow(new NegocioException(HttpStatus.CONFLICT, "No puedes saltarte etapas."));
        mvc.perform(put("/recomendaciones/1/estado").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void devuelveLosCatalogos() throws Exception {
        CatalogoService catalogo = mock(CatalogoService.class);
        when(catalogo.todos()).thenReturn(new CatalogosDto(List.of(new ItemCatalogoDto(11, "Deporte")),
                List.of(new ItemCatalogoDto(1, "Recibida"))));
        MockMvc otro = MvcSupport.mvc(new CatalogoController(catalogo));
        otro.perform(get("/recomendaciones/catalogos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rubros[0].nombre").value("Deporte"))
                .andExpect(jsonPath("$.estados[0].id").value(1));
    }
}
