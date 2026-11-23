package cl.duoc.pagoflex.pagos.controller;

import static cl.duoc.pagoflex.pagos.Datos.AHORA;
import static cl.duoc.pagoflex.pagos.Datos.HOY;
import static cl.duoc.pagoflex.pagos.Datos.compromiso;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.pagoflex.pagos.dto.AnularRequest;
import cl.duoc.pagoflex.pagos.dto.PagarRequest;
import cl.duoc.pagoflex.pagos.dto.RegistrarCompromisoRequest;
import cl.duoc.pagoflex.pagos.dto.VencimientosRequest;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import cl.duoc.pagoflex.pagos.model.Anulacion;
import cl.duoc.pagoflex.pagos.model.Pago;
import cl.duoc.pagoflex.pagos.service.CompromisoService;
import feign.FeignException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class CompromisoControllerTest {

    private CompromisoService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(CompromisoService.class);
        mvc = MvcSupport.mvc(new CompromisoController(servicio));
    }

    private String json(Object o) throws Exception {
        return MvcSupport.MAPPER.writeValueAsString(o);
    }

    @Test
    void listaCompromisosDeUnaPersona() throws Exception {
        when(servicio.listar(eq(1), isNull(), isNull())).thenReturn(List.of(compromiso(10, 1, 1, HOY)));
        mvc.perform(get("/pagos/compromisos").param("idUsuario", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCompromiso").value(10))
                .andExpect(jsonPath("$[0].fechaVencimiento").value("2026-10-06"));
    }

    @Test
    void listaCompromisosPorIds() throws Exception {
        when(servicio.listar(isNull(), isNull(), eq(List.of(10, 11)))).thenReturn(List.of(compromiso(10, 1, 1, HOY)));
        mvc.perform(get("/pagos/compromisos").param("ids", "10", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void obtieneUnCompromisoOResponde404() throws Exception {
        when(servicio.obtener(10)).thenReturn(compromiso(10, 1, 1, HOY));
        when(servicio.obtener(99)).thenThrow(new NegocioException(HttpStatus.NOT_FOUND, "No encontramos el compromiso."));
        mvc.perform(get("/pagos/compromisos/10")).andExpect(status().isOk()).andExpect(jsonPath("$.folio").value("CP-2026-00010"));
        mvc.perform(get("/pagos/compromisos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos el compromiso."));
    }

    @Test
    void registraUnCompromisoYResponde201() throws Exception {
        when(servicio.registrar(any(RegistrarCompromisoRequest.class))).thenReturn(compromiso(11, 1, 1, HOY));
        RegistrarCompromisoRequest p = new RegistrarCompromisoRequest(30, "12.345.678-5", "Ana", "Paz", "a@b.cl",
                "+56912345678", "Arriendo", 50_000, HOY.plusDays(10), 1, 1, HOY, AHORA);
        mvc.perform(post("/pagos/compromisos").contentType(MediaType.APPLICATION_JSON).content(json(p)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCompromiso").value(11));
    }

    @Test
    void registrarConErrorDeNegocioDevuelveElMensaje() throws Exception {
        when(servicio.registrar(any(RegistrarCompromisoRequest.class)))
                .thenThrow(new NegocioException(HttpStatus.BAD_REQUEST, "El RUT no es válido."));
        mvc.perform(post("/pagos/compromisos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El RUT no es válido."));
    }

    @Test
    void pagaUnCompromisoYDevuelveElComprobante() throws Exception {
        Pago pago = new Pago();
        pago.setIdPago(3);
        pago.setNumeroComprobante("CMP-0003");
        pago.setIdCompromiso(10);
        pago.setMontoPagado(50_000);
        pago.setFechaHora(AHORA);
        when(servicio.pagar(eq(10), any(PagarRequest.class))).thenReturn(pago);
        mvc.perform(post("/pagos/compromisos/10/pago").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PagarRequest(1, 4, 1, AHORA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroComprobante").value("CMP-0003"))
                .andExpect(jsonPath("$.montoPagado").value(50_000));
    }

    @Test
    void pagarUnCompromisoYaPagadoDevuelve409() throws Exception {
        when(servicio.pagar(eq(10), any(PagarRequest.class)))
                .thenThrow(new NegocioException(HttpStatus.CONFLICT, "Este compromiso ya fue pagado."));
        mvc.perform(post("/pagos/compromisos/10/pago").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void sinCuentasDisponiblesRespondeServicioNoDisponible() throws Exception {
        when(servicio.pagar(eq(10), any(PagarRequest.class))).thenThrow(mock(FeignException.class));
        mvc.perform(post("/pagos/compromisos/10/pago").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void anulaUnCompromiso() throws Exception {
        Anulacion a = new Anulacion();
        a.setIdCompromiso(10);
        a.setIdEjecutivo(30);
        a.setIdMotivoAnulacion(4);
        a.setComentario("Duplicado");
        a.setFecha(AHORA);
        when(servicio.anular(eq(10), any(AnularRequest.class))).thenReturn(a);
        mvc.perform(put("/pagos/compromisos/10/anulacion").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AnularRequest(30, 4, "Duplicado", AHORA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comentario").value("Duplicado"));
    }

    @Test
    void listaPagosYAnulaciones() throws Exception {
        Pago pago = new Pago();
        pago.setIdPago(1);
        Anulacion anulacion = new Anulacion();
        anulacion.setIdCompromiso(10);
        when(servicio.pagosDe(eq(1), isNull(), isNull())).thenReturn(List.of(pago));
        when(servicio.anulacionesDe(isNull(), eq(1), isNull())).thenReturn(List.of(anulacion));
        mvc.perform(get("/pagos/pagos").param("idUsuario", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPago").value(1));
        mvc.perform(get("/pagos/anulaciones").param("idEmpresa", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCompromiso").value(10));
    }

    @Test
    void actualizaVencimientos() throws Exception {
        when(servicio.actualizarVencimientos(any(VencimientosRequest.class))).thenReturn(3);
        mvc.perform(post("/pagos/vencimientos").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new VencimientosRequest(HOY, AHORA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizados").value(3));
        verify(servicio).actualizarVencimientos(any(VencimientosRequest.class));
    }
}
