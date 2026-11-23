package cl.duoc.pagoflex.recomendaciones.service;

import static cl.duoc.pagoflex.recomendaciones.Datos.AHORA;
import static cl.duoc.pagoflex.recomendaciones.Datos.HOY;
import static cl.duoc.pagoflex.recomendaciones.Datos.recomendacion;
import static cl.duoc.pagoflex.recomendaciones.Datos.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.pagoflex.recomendaciones.client.AuthGateway;
import cl.duoc.pagoflex.recomendaciones.dto.CambiarEstadoRequest;
import cl.duoc.pagoflex.recomendaciones.dto.NuevaRecomendacionRequest;
import cl.duoc.pagoflex.recomendaciones.dto.RecomendacionDto;
import cl.duoc.pagoflex.recomendaciones.dto.ResumenDto;
import cl.duoc.pagoflex.recomendaciones.exception.NegocioException;
import cl.duoc.pagoflex.recomendaciones.model.Catalogo;
import cl.duoc.pagoflex.recomendaciones.model.ContactoRecomendacion;
import cl.duoc.pagoflex.recomendaciones.model.HistorialRecomendacion;
import cl.duoc.pagoflex.recomendaciones.model.Recomendacion;
import cl.duoc.pagoflex.recomendaciones.repository.CatalogoRepository;
import cl.duoc.pagoflex.recomendaciones.repository.ContactoRepository;
import cl.duoc.pagoflex.recomendaciones.repository.HistorialRepository;
import cl.duoc.pagoflex.recomendaciones.repository.RecomendacionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecomendacionServiceTest {

    @Mock
    private RecomendacionRepository recomendaciones;
    @Mock
    private ContactoRepository contactos;
    @Mock
    private HistorialRepository historial;
    @Mock
    private CatalogoRepository catalogos;
    @Mock
    private AuthGateway auth;

    private RecomendacionService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneId.of("UTC"));
        servicio = new RecomendacionService(recomendaciones, contactos, historial, catalogos, auth, reloj);
        when(auth.usuario(1)).thenReturn(Optional.of(usuario(1, 1, true)));
        when(auth.usuario(2)).thenReturn(Optional.of(usuario(2, 1, false)));
        when(auth.usuario(9)).thenReturn(Optional.of(usuario(9, 2, true)));
        when(auth.usuario(10)).thenReturn(Optional.of(usuario(10, 2, false)));
        when(auth.usuario(30)).thenReturn(Optional.of(usuario(30, 3, true)));
        when(auth.usuario(404)).thenReturn(Optional.empty());
        when(auth.existeComuna(anyInt())).thenReturn(true);
        when(catalogos.existsByTipoAndCodigo(anyString(), anyInt())).thenReturn(true);
        when(catalogos.findByTipoAndCodigo(eq(ReglasRecomendacion.TIPO_ESTADO), anyInt())).thenAnswer(i -> {
            int codigo = i.getArgument(1);
            String[] nombres = {"", "Recibida", "Contactada", "En evaluación", "Se unió", "Descartada"};
            return Optional.of(new Catalogo(codigo, ReglasRecomendacion.TIPO_ESTADO, codigo, nombres[codigo]));
        });
        when(recomendaciones.save(any(Recomendacion.class))).thenAnswer(i -> {
            Recomendacion r = i.getArgument(0);
            if (r.getIdRecomendacion() == null) {
                r.setIdRecomendacion(7);
            }
            return r;
        });
    }

    private NegocioException falla(Runnable r) {
        return assertThrows(NegocioException.class, r::run);
    }

    private NuevaRecomendacionRequest nueva(Integer usuario, String org) {
        return new NuevaRecomendacionRequest(usuario, org, null, 11, 3, null, null, null, null, AHORA);
    }

    @Test
    void listaTodasOLasDeUnaPersonaConContactoEHistorial() {
        Recomendacion r1 = recomendacion(1, 1, 3);
        Recomendacion r2 = recomendacion(2, 1, 1);
        when(recomendaciones.findByIdUsuarioFinalOrderByFechaDesc(1)).thenReturn(List.of(r1, r2));
        when(recomendaciones.findAllByOrderByFechaDesc()).thenReturn(List.of(r1));
        when(contactos.findByIdRecomendacionIn(anyCollection())).thenReturn(List.of(
                new ContactoRecomendacion(1, "Raúl", null, "+56912345678", true),
                new ContactoRecomendacion(2, "Sin permiso", "a@b.cl", null, false)));
        when(historial.findByIdRecomendacionInOrderByFecha(anyCollection())).thenReturn(List.of(
                new HistorialRecomendacion(1, 1, 2, 9, AHORA, null)));
        List<RecomendacionDto> propias = servicio.listar(1);
        assertEquals(2, propias.size());
        assertEquals("Raúl", propias.get(0).contacto().nombre());
        assertNull(propias.get(1).contacto());
        assertEquals(1, propias.get(0).cambios().size());
        assertTrue(propias.get(1).cambios().isEmpty());
        assertEquals(1, servicio.listar(null).size());
    }

    @Test
    void obtenerUnaRecomendacionOResponde404() {
        when(recomendaciones.findById(1)).thenReturn(Optional.of(recomendacion(1, 1, 1)));
        when(recomendaciones.findById(99)).thenReturn(Optional.empty());
        assertEquals("REC-0001", servicio.obtener(1).numero());
        assertEquals(HttpStatus.NOT_FOUND, falla(() -> servicio.obtener(99)).getEstado());
    }

    @Test
    void elResumenCuentaElMesYLasQueSeUnieron() {
        when(recomendaciones.countByIdUsuarioFinalAndFechaGreaterThanEqualAndFechaLessThan(
                1, HOY.withDayOfMonth(1).atStartOfDay(), HOY.withDayOfMonth(1).plusMonths(1).atStartOfDay())).thenReturn(3L);
        when(recomendaciones.countByIdUsuarioFinalAndIdEstadoRecomendacion(1, ReglasRecomendacion.SE_UNIO)).thenReturn(2L);
        ResumenDto conFecha = servicio.resumen(1, HOY);
        ResumenDto sinFecha = servicio.resumen(1, null);
        assertEquals(3, conFecha.enviadasEsteMes());
        assertEquals(2, conFecha.seUnieron());
        assertEquals(3, sinFecha.enviadasEsteMes());
    }

    @Test
    void creaUnaRecomendacionSinContacto() {
        RecomendacionDto dto = servicio.crear(new NuevaRecomendacionRequest(1, "  Club   Los  Halcones ",
                "12.345.678-5", 6, 7, null, null, null, null, AHORA));
        assertEquals("REC-0001", dto.numero());
        assertEquals("Club Los Halcones", dto.nombreOrganizacion());
        assertEquals("12.345.678-5", dto.rutOrganizacion());
        assertEquals(ReglasRecomendacion.RECIBIDA, dto.idEstadoRecomendacion());
        assertNull(dto.contacto());
        verify(contactos, never()).save(any(ContactoRecomendacion.class));
    }

    @Test
    void creaUnaRecomendacionConContactoAutorizado() {
        Recomendacion ultima = recomendacion(6, 1, 1);
        when(recomendaciones.findTopByOrderByNumeroDesc()).thenReturn(Optional.of(ultima));
        when(contactos.findByIdRecomendacionIn(anyCollection())).thenReturn(List.of(
                new ContactoRecomendacion(7, "Ana", "a@b.cl", null, true)));
        RecomendacionDto dto = servicio.crear(new NuevaRecomendacionRequest(1, "Club", "", 6, 7, " Ana ", "a@b.cl", "",
                true, null));
        assertEquals("REC-0007", dto.numero());
        assertNull(dto.rutOrganizacion());
        ArgumentCaptor<ContactoRecomendacion> captor = ArgumentCaptor.forClass(ContactoRecomendacion.class);
        verify(contactos).save(captor.capture());
        assertEquals("Ana", captor.getValue().getNombre());
        assertNull(captor.getValue().getTelefono());
        assertNotNull(dto.contacto());
    }

    @Test
    void soloUnaPersonaActivaConCuentaPuedeRecomendar() {
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.crear(nueva(9, "Club"))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.crear(nueva(2, "Club"))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.crear(nueva(404, "Club"))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.crear(nueva(null, "Club"))).getEstado());
    }

    @Test
    void validaLosDatosDeLaRecomendacion() {
        assertEquals("Escribe el nombre de la organización.", falla(() -> servicio.crear(nueva(1, " "))).getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, falla(() -> servicio.crear(nueva(1, null))).getEstado());
        when(catalogos.existsByTipoAndCodigo(ReglasRecomendacion.TIPO_RUBRO, 11)).thenReturn(false);
        assertEquals("Elige el rubro y la comuna de la organización.", falla(() -> servicio.crear(nueva(1, "Club"))).getMessage());
        when(catalogos.existsByTipoAndCodigo(ReglasRecomendacion.TIPO_RUBRO, 11)).thenReturn(true);
        when(auth.existeComuna(3)).thenReturn(false);
        assertEquals(HttpStatus.BAD_REQUEST, falla(() -> servicio.crear(nueva(1, "Club"))).getEstado());
    }

    @Test
    void elContactoSinAutorizacionSeRechaza() {
        NuevaRecomendacionRequest p = new NuevaRecomendacionRequest(1, "Club", null, 11, 3, "Ana", "a@b.cl", null,
                false, AHORA);
        assertTrue(falla(() -> servicio.crear(p)).getMessage().startsWith("Para guardar estos datos"));
    }

    @Test
    void noSePuedeRecomendarDosVecesLaMismaOrganizacion() {
        when(recomendaciones.findByIdUsuarioFinalAndNombreOrganizacionIgnoreCase(1, "Gimnasio Fuerza Sur"))
                .thenReturn(Optional.of(recomendacion(3, 1, 2)));
        NegocioException e = falla(() -> servicio.crear(nueva(1, "gimnasio   fuerza sur")));
        assertEquals(HttpStatus.CONFLICT, e.getEstado());
        assertTrue(e.getMessage().contains("REC-0003"));
        assertTrue(e.getMessage().contains("Contactada"));
    }

    @Test
    void elLimiteEsDeCincoPorMes() {
        when(recomendaciones.countByIdUsuarioFinalAndFechaGreaterThanEqualAndFechaLessThan(eq(1), any(), any()))
                .thenReturn(5L);
        NegocioException e = falla(() -> servicio.crear(nueva(1, "Club")));
        assertEquals(HttpStatus.CONFLICT, e.getEstado());
        assertTrue(e.getMessage().contains("5 recomendaciones"));
        when(recomendaciones.countByIdUsuarioFinalAndFechaGreaterThanEqualAndFechaLessThan(eq(1), any(), any()))
                .thenReturn(4L);
        assertEquals("Club", servicio.crear(nueva(1, "Club")).nombreOrganizacion());
    }

    @Test
    void sinFechaPedidaUsaElRelojDelServidor() {
        RecomendacionDto dto = servicio.crear(new NuevaRecomendacionRequest(1, "Club", null, 11, 3, null, null, null,
                null, null));
        assertEquals(AHORA, dto.fecha());
    }

    private Recomendacion enEstado(int id, int estado) {
        Recomendacion r = recomendacion(id, 1, estado);
        when(recomendaciones.findById(id)).thenReturn(Optional.of(r));
        return r;
    }

    private CambiarEstadoRequest cambio(Integer agente, Integer estado, String motivo) {
        return new CambiarEstadoRequest(agente, estado, motivo, AHORA);
    }

    @Test
    void unAgenteAvanzaEtapaPorEtapa() {
        Recomendacion r = enEstado(1, ReglasRecomendacion.RECIBIDA);
        servicio.cambiarEstado(1, cambio(9, ReglasRecomendacion.CONTACTADA, "ignorado"));
        assertEquals(ReglasRecomendacion.CONTACTADA, r.getIdEstadoRecomendacion());
        ArgumentCaptor<HistorialRecomendacion> captor = ArgumentCaptor.forClass(HistorialRecomendacion.class);
        verify(historial).save(captor.capture());
        assertEquals(9, captor.getValue().getIdAgente());
        assertNull(captor.getValue().getMotivo());
    }

    @Test
    void descartarGuardaElMotivo() {
        Recomendacion r = enEstado(1, ReglasRecomendacion.EN_EVALUACION);
        servicio.cambiarEstado(1, cambio(9, ReglasRecomendacion.DESCARTADA, "  Ya es cliente  "));
        assertEquals(ReglasRecomendacion.DESCARTADA, r.getIdEstadoRecomendacion());
        ArgumentCaptor<HistorialRecomendacion> captor = ArgumentCaptor.forClass(HistorialRecomendacion.class);
        verify(historial).save(captor.capture());
        assertEquals("Ya es cliente", captor.getValue().getMotivo());
    }

    @Test
    void descartarExigeMotivoDeHastaDosCientosCincuentaYCincoLetras() {
        enEstado(1, ReglasRecomendacion.EN_EVALUACION);
        assertEquals("Escribe el motivo para descartar la recomendación.",
                falla(() -> servicio.cambiarEstado(1, cambio(9, 5, " "))).getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, falla(() -> servicio.cambiarEstado(1, cambio(9, 5, null))).getEstado());
        assertTrue(falla(() -> servicio.cambiarEstado(1, cambio(9, 5, "x".repeat(256)))).getMessage().contains("255"));
    }

    @Test
    void noSePuedeSaltarEtapasNiCambiarUnaCerrada() {
        enEstado(1, ReglasRecomendacion.RECIBIDA);
        NegocioException salto = falla(() -> servicio.cambiarEstado(1, cambio(9, ReglasRecomendacion.SE_UNIO, null)));
        assertEquals(HttpStatus.CONFLICT, salto.getEstado());
        assertTrue(salto.getMessage().contains("«Recibida»"));
        assertTrue(salto.getMessage().contains("«Contactada»"));
        enEstado(2, ReglasRecomendacion.EN_EVALUACION);
        assertTrue(falla(() -> servicio.cambiarEstado(2, cambio(9, 2, null))).getMessage().contains(" o "));
        enEstado(3, ReglasRecomendacion.SE_UNIO);
        assertTrue(falla(() -> servicio.cambiarEstado(3, cambio(9, 5, "x"))).getMessage().contains("ya está cerrada"));
    }

    @Test
    void soloUnAgenteActivoPuedeCambiarEstados() {
        enEstado(1, ReglasRecomendacion.RECIBIDA);
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.cambiarEstado(1, cambio(1, 2, null))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.cambiarEstado(1, cambio(10, 2, null))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.cambiarEstado(1, cambio(30, 2, null))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.cambiarEstado(1, cambio(404, 2, null))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.cambiarEstado(1, cambio(null, 2, null))).getEstado());
    }

    @Test
    void cambiarEstadoValidaExistenciaYEstadoElegido() {
        when(recomendaciones.findById(99)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, falla(() -> servicio.cambiarEstado(99, cambio(9, 2, null))).getEstado());
        enEstado(1, ReglasRecomendacion.RECIBIDA);
        assertEquals("Elige el nuevo estado.", falla(() -> servicio.cambiarEstado(1, cambio(9, null, null))).getMessage());
        when(catalogos.existsByTipoAndCodigo(ReglasRecomendacion.TIPO_ESTADO, 8)).thenReturn(false);
        assertEquals(HttpStatus.BAD_REQUEST, falla(() -> servicio.cambiarEstado(1, cambio(9, 8, null))).getEstado());
    }

    @Test
    void sinFechaPedidaElHistorialUsaElRelojDelServidor() {
        enEstado(1, ReglasRecomendacion.RECIBIDA);
        servicio.cambiarEstado(1, new CambiarEstadoRequest(9, 2, null, null));
        ArgumentCaptor<HistorialRecomendacion> captor = ArgumentCaptor.forClass(HistorialRecomendacion.class);
        verify(historial).save(captor.capture());
        assertEquals(AHORA, captor.getValue().getFecha());
    }
}
