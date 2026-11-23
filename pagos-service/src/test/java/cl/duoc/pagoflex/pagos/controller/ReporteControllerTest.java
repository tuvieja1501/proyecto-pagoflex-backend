package cl.duoc.pagoflex.pagos.controller;

import static cl.duoc.pagoflex.pagos.Datos.AHORA;
import static cl.duoc.pagoflex.pagos.Datos.HOY;
import static cl.duoc.pagoflex.pagos.Datos.compromiso;
import static cl.duoc.pagoflex.pagos.Datos.reporte;
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

import cl.duoc.pagoflex.pagos.dto.NuevoReporteRequest;
import cl.duoc.pagoflex.pagos.dto.ResolverReporteRequest;
import cl.duoc.pagoflex.pagos.dto.TomarReporteRequest;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import cl.duoc.pagoflex.pagos.service.ReporteService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class ReporteControllerTest {

    private ReporteService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(ReporteService.class);
        mvc = MvcSupport.mvc(new ReporteController(servicio));
    }

    private String json(Object o) throws Exception {
        return MvcSupport.MAPPER.writeValueAsString(o);
    }

    @Test
    void listaReportes() throws Exception {
        when(servicio.listar(isNull(), isNull())).thenReturn(List.of(reporte(1, 10, 1), reporte(2, 11, 2)));
        mvc.perform(get("/pagos/reportes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].numero").value("REP-0002"));
    }

    @Test
    void listaReportesDeUnaPersonaOCompromiso() throws Exception {
        when(servicio.listar(eq(1), isNull())).thenReturn(List.of(reporte(1, 10, 1)));
        when(servicio.listar(isNull(), eq(10))).thenReturn(List.of(reporte(1, 10, 1)));
        mvc.perform(get("/pagos/reportes").param("idUsuario", "1")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/pagos/reportes").param("idCompromiso", "10")).andExpect(jsonPath("$[0].idCompromiso").value(10));
    }

    @Test
    void obtieneUnReporteOResponde404() throws Exception {
        when(servicio.obtener(1)).thenReturn(reporte(1, 10, 1));
        when(servicio.obtener(9)).thenThrow(new NegocioException(HttpStatus.NOT_FOUND, "No encontramos el reporte."));
        mvc.perform(get("/pagos/reportes/1")).andExpect(status().isOk()).andExpect(jsonPath("$.idReporte").value(1));
        mvc.perform(get("/pagos/reportes/9")).andExpect(status().isNotFound());
    }

    @Test
    void creaUnReporteYResponde201() throws Exception {
        when(servicio.crear(any(NuevoReporteRequest.class))).thenReturn(reporte(5, 10, 1));
        mvc.perform(post("/pagos/reportes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NuevoReporteRequest(1, 10, 1, "No lo reconozco", AHORA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idReporte").value(5));
    }

    @Test
    void crearReporteDuplicadoDevuelve409() throws Exception {
        when(servicio.crear(any(NuevoReporteRequest.class)))
                .thenThrow(new NegocioException(HttpStatus.CONFLICT, "Este compromiso ya tiene un reporte en curso."));
        mvc.perform(post("/pagos/reportes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Este compromiso ya tiene un reporte en curso."));
    }

    @Test
    void unAgenteTomaUnReporte() throws Exception {
        when(servicio.tomar(eq(1), any(TomarReporteRequest.class))).thenReturn(reporte(1, 10, 2));
        mvc.perform(put("/pagos/reportes/1/toma").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TomarReporteRequest(20, AHORA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEstadoReporte").value(2));
    }

    @Test
    void resolverDevuelveElCompromisoActualizado() throws Exception {
        when(servicio.resolver(eq(1), any(ResolverReporteRequest.class))).thenReturn(compromiso(10, 1, 1, HOY));
        mvc.perform(put("/pagos/reportes/1/resolucion").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ResolverReporteRequest(20, "Listo", HOY, AHORA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCompromiso").value(10))
                .andExpect(jsonPath("$.idEstadoCompromiso").value(1));
    }

    @Test
    void resolverSinPermisoDevuelve403() throws Exception {
        when(servicio.resolver(eq(1), any(ResolverReporteRequest.class)))
                .thenThrow(new NegocioException(HttpStatus.FORBIDDEN, "Solo un agente PagoFlex activo puede gestionar reportes."));
        mvc.perform(put("/pagos/reportes/1/resolucion").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
