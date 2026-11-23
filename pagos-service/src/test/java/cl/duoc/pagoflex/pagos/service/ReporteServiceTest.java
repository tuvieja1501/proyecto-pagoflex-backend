package cl.duoc.pagoflex.pagos.service;

import static cl.duoc.pagoflex.pagos.Datos.AHORA;
import static cl.duoc.pagoflex.pagos.Datos.HOY;
import static cl.duoc.pagoflex.pagos.Datos.compromiso;
import static cl.duoc.pagoflex.pagos.Datos.reporte;
import static cl.duoc.pagoflex.pagos.Datos.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.pagoflex.pagos.client.AuthGateway;
import cl.duoc.pagoflex.pagos.dto.NuevoReporteRequest;
import cl.duoc.pagoflex.pagos.dto.ResolverReporteRequest;
import cl.duoc.pagoflex.pagos.dto.TomarReporteRequest;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import cl.duoc.pagoflex.pagos.model.Compromiso;
import cl.duoc.pagoflex.pagos.model.EmpresaCliente;
import cl.duoc.pagoflex.pagos.model.Reporte;
import cl.duoc.pagoflex.pagos.repository.CatalogoRepository;
import cl.duoc.pagoflex.pagos.repository.CompromisoRepository;
import cl.duoc.pagoflex.pagos.repository.EmpresaRepository;
import cl.duoc.pagoflex.pagos.repository.ReporteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
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
class ReporteServiceTest {

    @Mock
    private ReporteRepository reportes;
    @Mock
    private CompromisoRepository compromisos;
    @Mock
    private EmpresaRepository empresas;
    @Mock
    private CatalogoRepository catalogos;
    @Mock
    private AuthGateway auth;

    private ReporteService servicio;
    private Compromiso pendiente;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneId.of("UTC"));
        servicio = new ReporteService(reportes, compromisos, empresas, catalogos, auth, reloj);
        pendiente = compromiso(10, 1, ReglasPagos.PENDIENTE, HOY.plusDays(5));
        when(compromisos.findById(10)).thenReturn(Optional.of(pendiente));
        when(compromisos.findById(99)).thenReturn(Optional.empty());
        when(catalogos.existsByTipoAndCodigo(anyString(), anyInt())).thenReturn(true);
        when(reportes.save(any(Reporte.class))).thenAnswer(i -> {
            Reporte r = i.getArgument(0);
            if (r.getIdReporte() == null) {
                r.setIdReporte(5);
            }
            return r;
        });
        when(auth.usuario(20)).thenReturn(Optional.of(usuario(20, 2, null)));
        when(auth.usuario(21)).thenReturn(Optional.of(usuario(21, 2, null)));
        when(auth.usuario(1)).thenReturn(Optional.of(usuario(1, 1, null)));
        when(auth.usuario(404)).thenReturn(Optional.empty());
    }

    private NegocioException falla(Runnable r) {
        return assertThrows(NegocioException.class, r::run);
    }

    private NuevoReporteRequest nuevo(Integer usuario, Integer compromiso, Integer motivo, String comentario) {
        return new NuevoReporteRequest(usuario, compromiso, motivo, comentario, AHORA);
    }

    @Test
    void listarPorCompromisoPorUsuarioOTodos() {
        when(reportes.findByIdCompromisoIn(anyCollection())).thenReturn(List.of(reporte(1, 10, 1)));
        when(compromisos.findByIdUsuarioFinalOrderByFechaVencimiento(1)).thenReturn(List.of(pendiente));
        when(reportes.findAll()).thenReturn(List.of(reporte(1, 10, 1), reporte(2, 11, 2)));
        assertEquals(1, servicio.listar(null, 10).size());
        assertEquals(1, servicio.listar(1, null).size());
        assertEquals(2, servicio.listar(null, null).size());
    }

    @Test
    void obtenerInexistenteDa404() {
        when(reportes.findById(7)).thenReturn(Optional.empty());
        when(reportes.findById(1)).thenReturn(Optional.of(reporte(1, 10, 1)));
        assertEquals(HttpStatus.NOT_FOUND, falla(() -> servicio.obtener(7)).getEstado());
        assertEquals(1, servicio.obtener(1).getIdReporte());
    }

    @Test
    void crearReportePoneElCompromisoEnRevision() {
        Reporte r = servicio.crear(nuevo(1, 10, 1, "  No lo reconozco  "));
        assertEquals("REP-0001", r.getNumero());
        assertEquals("No lo reconozco", r.getComentario());
        assertEquals(ReglasPagos.REPORTE_ABIERTO, r.getIdEstadoReporte());
        assertEquals(ReglasPagos.EN_REVISION, pendiente.getIdEstadoCompromiso());
    }

    @Test
    void crearReporteContinuaLaNumeracionYGuardaComentarioNulo() {
        Reporte ultimo = reporte(8, 3, 3);
        ultimo.setNumero("REP-0008");
        when(reportes.findTopByOrderByNumeroDesc()).thenReturn(Optional.of(ultimo));
        Reporte r = servicio.crear(nuevo(1, 10, 1, null));
        assertEquals("REP-0009", r.getNumero());
        assertNull(r.getComentario());
    }

    @Test
    void crearReporteValidaMotivoYComentario() {
        assertEquals("Elige el motivo de tu reporte.", falla(() -> servicio.crear(nuevo(1, 10, null, "x"))).getMessage());
        when(catalogos.existsByTipoAndCodigo(ReglasPagos.TIPO_MOTIVO_REPORTE, 77)).thenReturn(false);
        assertEquals(HttpStatus.BAD_REQUEST, falla(() -> servicio.crear(nuevo(1, 10, 77, "x"))).getEstado());
        assertEquals("Cuéntanos qué pasó para poder ayudarte.", falla(() -> servicio.crear(nuevo(1, 10, 4, " "))).getMessage());
        assertTrue(falla(() -> servicio.crear(nuevo(1, 10, 1, "x".repeat(501)))).getMessage().contains("500"));
    }

    @Test
    void crearReporteDeOtraPersonaOCompromisoInexistenteDa403() {
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.crear(nuevo(2, 10, 1, "x"))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.crear(nuevo(1, 99, 1, "x"))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.crear(nuevo(1, null, 1, "x"))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.crear(nuevo(null, 10, 1, "x"))).getEstado());
    }

    @Test
    void noSePuedeReportarDosVecesNiUnCompromisoPagado() {
        when(reportes.findFirstByIdCompromisoAndIdEstadoReporteNot(10, ReglasPagos.REPORTE_RESUELTO))
                .thenReturn(Optional.of(reporte(2, 10, 1)));
        assertTrue(falla(() -> servicio.crear(nuevo(1, 10, 1, "x"))).getMessage().contains("REP-0002"));
        when(reportes.findFirstByIdCompromisoAndIdEstadoReporteNot(10, ReglasPagos.REPORTE_RESUELTO))
                .thenReturn(Optional.empty());
        pendiente.setIdEstadoCompromiso(ReglasPagos.PAGADO);
        assertEquals("Solo se pueden reportar compromisos pendientes o vencidos.",
                falla(() -> servicio.crear(nuevo(1, 10, 1, "x"))).getMessage());
    }

    @Test
    void tomarUnReporteAbierto() {
        Reporte abierto = reporte(1, 10, ReglasPagos.REPORTE_ABIERTO);
        when(reportes.findById(1)).thenReturn(Optional.of(abierto));
        Reporte r = servicio.tomar(1, new TomarReporteRequest(20, AHORA));
        assertEquals(ReglasPagos.REPORTE_EN_GESTION, r.getIdEstadoReporte());
        assertEquals(20, r.getIdAgente());
        assertEquals(AHORA, r.getFechaToma());
    }

    @Test
    void tomarExigeAgenteActivoYReporteAbierto() {
        when(reportes.findById(1)).thenReturn(Optional.of(reporte(1, 10, ReglasPagos.REPORTE_ABIERTO)));
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.tomar(1, new TomarReporteRequest(1, AHORA))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.tomar(1, new TomarReporteRequest(404, AHORA))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.tomar(1, new TomarReporteRequest(null, AHORA))).getEstado());
        when(reportes.findById(2)).thenReturn(Optional.of(reporte(2, 10, ReglasPagos.REPORTE_EN_GESTION)));
        assertEquals("Este reporte ya está en gestión.", falla(() -> servicio.tomar(2, new TomarReporteRequest(20, AHORA))).getMessage());
        when(reportes.findById(3)).thenReturn(Optional.of(reporte(3, 10, ReglasPagos.REPORTE_RESUELTO)));
        assertEquals("Este reporte ya está resuelto.", falla(() -> servicio.tomar(3, new TomarReporteRequest(20, AHORA))).getMessage());
    }

    private Reporte enGestion(int id, int agente) {
        Reporte r = reporte(id, 10, ReglasPagos.REPORTE_EN_GESTION);
        r.setIdAgente(agente);
        when(reportes.findById(id)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void resolverVuelveElCompromisoAPendienteSiAunNoVence() {
        pendiente.setIdEstadoCompromiso(ReglasPagos.EN_REVISION);
        Reporte r = enGestion(1, 20);
        Compromiso c = servicio.resolver(1, new ResolverReporteRequest(20, "  Todo en orden  ", HOY, AHORA));
        assertEquals(ReglasPagos.PENDIENTE, c.getIdEstadoCompromiso());
        assertEquals(ReglasPagos.REPORTE_RESUELTO, r.getIdEstadoReporte());
        assertEquals("Todo en orden", r.getRespuesta());
        verify(compromisos).save(pendiente);
    }

    @Test
    void resolverUnCompromisoYaVencidoAplicaElRecargo() {
        pendiente.setIdEstadoCompromiso(ReglasPagos.EN_REVISION);
        pendiente.setFechaVencimiento(HOY.minusDays(2));
        EmpresaCliente empresa = new EmpresaCliente();
        empresa.setRecargoAtraso(1_200);
        when(empresas.findById(1)).thenReturn(Optional.of(empresa));
        enGestion(1, 20);
        Compromiso c = servicio.resolver(1, new ResolverReporteRequest(20, "Resuelto", null, null));
        assertEquals(ReglasPagos.VENCIDO, c.getIdEstadoCompromiso());
        assertEquals(1_200, c.getRecargoAplicado());
    }

    @Test
    void resolverSinCambiosEnElCompromisoNoLoGuarda() {
        pendiente.setIdEstadoCompromiso(ReglasPagos.PAGADO);
        enGestion(1, 20);
        servicio.resolver(1, new ResolverReporteRequest(20, "Resuelto", HOY, AHORA));
        org.mockito.Mockito.verify(compromisos, org.mockito.Mockito.never()).save(any(Compromiso.class));
    }

    @Test
    void resolverValidaAgenteEstadoYRespuesta() {
        assertEquals(HttpStatus.FORBIDDEN,
                falla(() -> servicio.resolver(1, new ResolverReporteRequest(1, "x", HOY, AHORA))).getEstado());
        when(reportes.findById(1)).thenReturn(Optional.of(reporte(1, 10, ReglasPagos.REPORTE_ABIERTO)));
        assertEquals("Primero toma el reporte para poder resolverlo.",
                falla(() -> servicio.resolver(1, new ResolverReporteRequest(20, "x", HOY, AHORA))).getMessage());
        enGestion(2, 21);
        assertTrue(falla(() -> servicio.resolver(2, new ResolverReporteRequest(20, "x", HOY, AHORA)))
                .getMessage().startsWith("Este reporte lo está gestionando Camila Rojas"));
        enGestion(3, 20);
        assertEquals(HttpStatus.BAD_REQUEST,
                falla(() -> servicio.resolver(3, new ResolverReporteRequest(20, " ", HOY, AHORA))).getEstado());
        assertEquals(HttpStatus.BAD_REQUEST,
                falla(() -> servicio.resolver(3, new ResolverReporteRequest(20, null, HOY, AHORA))).getEstado());
        assertTrue(falla(() -> servicio.resolver(3, new ResolverReporteRequest(20, "x".repeat(501), HOY, AHORA)))
                .getMessage().contains("500"));
    }

    @Test
    void resolverReporteConResponsableDesconocidoNombraAOtroAgente() {
        Reporte r = reporte(4, 10, ReglasPagos.REPORTE_EN_GESTION);
        r.setIdAgente(404);
        when(reportes.findById(4)).thenReturn(Optional.of(r));
        assertTrue(falla(() -> servicio.resolver(4, new ResolverReporteRequest(20, "x", HOY, AHORA)))
                .getMessage().contains("otro agente"));
    }

    @Test
    void resolverReporteDeCompromisoInexistenteDa404() {
        Reporte r = reporte(6, 99, ReglasPagos.REPORTE_EN_GESTION);
        r.setIdAgente(20);
        when(reportes.findById(6)).thenReturn(Optional.of(r));
        assertEquals(HttpStatus.NOT_FOUND,
                falla(() -> servicio.resolver(6, new ResolverReporteRequest(20, "x", HOY, AHORA))).getEstado());
    }
}
