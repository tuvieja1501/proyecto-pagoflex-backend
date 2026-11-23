package cl.duoc.pagoflex.pagos.service;

import static cl.duoc.pagoflex.pagos.Datos.AHORA;
import static cl.duoc.pagoflex.pagos.Datos.HOY;
import static cl.duoc.pagoflex.pagos.Datos.compromiso;
import static cl.duoc.pagoflex.pagos.Datos.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.pagoflex.pagos.client.AuthGateway;
import cl.duoc.pagoflex.pagos.dto.AnularRequest;
import cl.duoc.pagoflex.pagos.dto.PagarRequest;
import cl.duoc.pagoflex.pagos.dto.RegistrarCompromisoRequest;
import cl.duoc.pagoflex.pagos.dto.UsuarioRemoto;
import cl.duoc.pagoflex.pagos.dto.VencimientosRequest;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import cl.duoc.pagoflex.pagos.model.Anulacion;
import cl.duoc.pagoflex.pagos.model.Compromiso;
import cl.duoc.pagoflex.pagos.model.EmpresaCliente;
import cl.duoc.pagoflex.pagos.model.Pago;
import cl.duoc.pagoflex.pagos.repository.AnulacionRepository;
import cl.duoc.pagoflex.pagos.repository.CatalogoRepository;
import cl.duoc.pagoflex.pagos.repository.CompromisoRepository;
import cl.duoc.pagoflex.pagos.repository.EmpresaRepository;
import cl.duoc.pagoflex.pagos.repository.PagoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
class CompromisoServiceTest {

    @Mock
    private CompromisoRepository compromisos;
    @Mock
    private PagoRepository pagos;
    @Mock
    private AnulacionRepository anulaciones;
    @Mock
    private EmpresaRepository empresas;
    @Mock
    private CatalogoRepository catalogos;
    @Mock
    private AuthGateway auth;

    private CompromisoService servicio;
    private Compromiso pendiente;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneId.of("UTC"));
        servicio = new CompromisoService(compromisos, pagos, anulaciones, empresas, catalogos, auth, reloj);
        pendiente = compromiso(10, 1, ReglasPagos.PENDIENTE, HOY.plusDays(5));
        when(auth.usuario(1)).thenReturn(Optional.of(usuario(1, 1, null)));
        when(auth.usuario(5)).thenReturn(Optional.of(usuario(5, 3, 7)));
        when(auth.usuario(404)).thenReturn(Optional.empty());
        when(compromisos.findById(10)).thenReturn(Optional.of(pendiente));
        when(compromisos.findById(99)).thenReturn(Optional.empty());
        when(compromisos.save(any(Compromiso.class))).thenAnswer(i -> {
            Compromiso c = i.getArgument(0);
            if (c.getIdCompromiso() == null) {
                c.setIdCompromiso(77);
            }
            return c;
        });
        when(pagos.save(any(Pago.class))).thenAnswer(i -> {
            Pago p = i.getArgument(0);
            p.setIdPago(3);
            return p;
        });
        when(anulaciones.save(any(Anulacion.class))).thenAnswer(i -> i.getArgument(0));
        when(catalogos.existsByTipoAndCodigo(anyString(), anyInt())).thenReturn(true);
    }

    private PagarRequest pagar(Integer usuario, Integer medio) {
        return new PagarRequest(usuario, medio, null, AHORA);
    }

    private NegocioException falla(Runnable r) {
        return assertThrows(NegocioException.class, r::run);
    }

    @Test
    void listarElijeElFiltroCorrecto() {
        when(compromisos.findByIdUsuarioFinalOrderByFechaVencimiento(1)).thenReturn(List.of(pendiente));
        when(compromisos.findByIdEmpresaOrderByFechaVencimiento(1)).thenReturn(List.of(pendiente, pendiente));
        when(compromisos.findAllById(anyCollection())).thenReturn(List.of());
        when(compromisos.findAll()).thenReturn(List.of(pendiente, pendiente, pendiente));
        assertEquals(1, servicio.listar(1, null, null).size());
        assertEquals(2, servicio.listar(null, 1, null).size());
        assertEquals(0, servicio.listar(null, null, List.of(4)).size());
        assertEquals(3, servicio.listar(null, null, null).size());
    }

    @Test
    void obtenerInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, falla(() -> servicio.obtener(99)).getEstado());
        assertEquals(10, servicio.obtener(10).getIdCompromiso());
    }

    @Test
    void pagosYAnulacionesPorCompromisoOPorUsuario() {
        when(compromisos.findByIdUsuarioFinalOrderByFechaVencimiento(1)).thenReturn(List.of(pendiente));
        when(pagos.findByIdCompromisoIn(anyCollection())).thenReturn(List.of(new Pago()));
        when(anulaciones.findByIdCompromisoIn(anyCollection())).thenReturn(List.of(new Anulacion(), new Anulacion()));
        assertEquals(1, servicio.pagosDe(null, null, 10).size());
        assertEquals(1, servicio.pagosDe(1, null, null).size());
        assertEquals(2, servicio.anulacionesDe(null, null, 10).size());
        assertEquals(2, servicio.anulacionesDe(1, null, null).size());
    }

    @Test
    void pagarCreaComprobanteYMarcaPagado() {
        Pago p = servicio.pagar(10, pagar(1, 4));
        assertEquals("CPR-000001", p.getNumeroComprobante());
        assertEquals(50_000, p.getMontoPagado());
        assertEquals(ReglasPagos.CANAL_APP, p.getIdCanal());
        assertEquals(ReglasPagos.PAGADO, pendiente.getIdEstadoCompromiso());
        assertEquals(AHORA, pendiente.getFechaCambioEstado());
    }

    @Test
    void pagarUnCompromisoVencidoIncluyeElRecargo() {
        pendiente.setIdEstadoCompromiso(ReglasPagos.VENCIDO);
        pendiente.setRecargoAplicado(2_500);
        Pago p = servicio.pagar(10, pagar(1, 4));
        assertEquals(52_500, p.getMontoPagado());
    }

    @Test
    void pagarContinuaLaNumeracionDelUltimoComprobante() {
        Pago ultimo = new Pago();
        ultimo.setNumeroComprobante("CPR-000041");
        when(pagos.findTopByOrderByNumeroComprobanteDesc()).thenReturn(Optional.of(ultimo));
        assertEquals("CPR-000042", servicio.pagar(10, pagar(1, 4)).getNumeroComprobante());
    }

    @Test
    void pagarExigeUnaPersonaConCuentaDePagos() {
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.pagar(10, pagar(null, 4))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.pagar(10, pagar(404, 4))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.pagar(10, pagar(5, 4))).getEstado());
    }

    @Test
    void pagarElCompromisoDeOtraPersonaDa403() {
        pendiente.setIdUsuarioFinal(2);
        assertEquals("Este compromiso no corresponde a tu cuenta.", falla(() -> servicio.pagar(10, pagar(1, 4))).getMessage());
    }

    @Test
    void pagarCompromisoInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, falla(() -> servicio.pagar(99, pagar(1, 4))).getEstado());
    }

    @Test
    void noSePuedePagarPagadoAnuladoOEnRevision() {
        pendiente.setIdEstadoCompromiso(ReglasPagos.PAGADO);
        assertTrue(falla(() -> servicio.pagar(10, pagar(1, 4))).getMessage().contains("ya está pagado"));
        pendiente.setIdEstadoCompromiso(ReglasPagos.ANULADO);
        assertTrue(falla(() -> servicio.pagar(10, pagar(1, 4))).getMessage().contains("sin efecto"));
        pendiente.setIdEstadoCompromiso(ReglasPagos.EN_REVISION);
        assertTrue(falla(() -> servicio.pagar(10, pagar(1, 4))).getMessage().contains("en revisión"));
        verify(pagos, never()).save(any(Pago.class));
    }

    @Test
    void noSePuedePagarDosVeces() {
        when(pagos.existsByIdCompromiso(10)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, falla(() -> servicio.pagar(10, pagar(1, 4))).getEstado());
    }

    @Test
    void pagarValidaMedioDePagoYCanal() {
        assertEquals("Elige un medio de pago para continuar.", falla(() -> servicio.pagar(10, pagar(1, null))).getMessage());
        when(catalogos.existsByTipoAndCodigo(ReglasPagos.TIPO_MEDIO_PAGO, 9)).thenReturn(false);
        assertEquals(HttpStatus.BAD_REQUEST, falla(() -> servicio.pagar(10, pagar(1, 9))).getEstado());
        when(catalogos.existsByTipoAndCodigo(ReglasPagos.TIPO_CANAL, 8)).thenReturn(false);
        assertEquals("El canal de pago no existe.",
                falla(() -> servicio.pagar(10, new PagarRequest(1, 4, 8, AHORA))).getMessage());
    }

    @Test
    void pagarSinFechaUsaElRelojDelServidor() {
        Pago p = servicio.pagar(10, new PagarRequest(1, 4, 2, null));
        assertEquals(AHORA, p.getFechaHora());
        assertEquals(2, p.getIdCanal());
    }

    private RegistrarCompromisoRequest registro(String rut, LocalDate vence, Integer cuota, Integer total) {
        return new RegistrarCompromisoRequest(5, rut, "Benjamín", "Araya", "b@correo.ejemplo", "+56912345678",
                "Cuota arriendo", 50_000, vence, cuota, total, HOY, AHORA);
    }

    @Test
    void registrarParaUnaPersonaExistente() {
        when(auth.porRut("12.345.678-5")).thenReturn(Optional.of(usuario(1, 1, null)));
        Compromiso c = servicio.registrar(registro("12.345.678-5", HOY.plusDays(10), 1, 3));
        assertEquals("CP-2026-0001", c.getFolio());
        assertEquals(1, c.getIdUsuarioFinal());
        assertEquals(7, c.getIdEmpresa());
        assertEquals(ReglasPagos.PENDIENTE, c.getIdEstadoCompromiso());
        assertEquals(5, c.getIdEjecutivoRegistro());
    }

    @Test
    void registrarContinuaElFolioDelAnio() {
        when(auth.porRut("12.345.678-5")).thenReturn(Optional.of(usuario(1, 1, null)));
        Compromiso previo = compromiso(1, 1, 1, HOY);
        previo.setFolio("CP-2026-0018");
        when(compromisos.findTopByFolioStartingWithOrderByFolioDesc("CP-2026-")).thenReturn(Optional.of(previo));
        assertEquals("CP-2026-0019", servicio.registrar(registro("12.345.678-5", HOY, null, null)).getFolio());
    }

    @Test
    void registrarCreaALaPersonaSiNoExiste() {
        when(auth.porRut("12.345.678-5")).thenReturn(Optional.empty());
        when(auth.crearPersona(any())).thenReturn(usuario(31, 1, null));
        Compromiso c = servicio.registrar(registro("123456785", HOY, null, null));
        assertEquals(31, c.getIdUsuarioFinal());
        verify(auth).crearPersona(any());
    }

    @Test
    void registrarPersonaNuevaExigeDatosValidos() {
        when(auth.porRut("12.345.678-5")).thenReturn(Optional.empty());
        RegistrarCompromisoRequest sinNombre = new RegistrarCompromisoRequest(5, "12.345.678-5", " ", "Araya",
                "b@c.cl", "+56912345678", "Arriendo", 1000, HOY, null, null, HOY, AHORA);
        assertEquals("Escribe el nombre y los apellidos de la persona.", falla(() -> servicio.registrar(sinNombre)).getMessage());
        RegistrarCompromisoRequest sinApellido = new RegistrarCompromisoRequest(5, "12.345.678-5", "Ana", null,
                "b@c.cl", "+56912345678", "Arriendo", 1000, HOY, null, null, HOY, AHORA);
        assertEquals(HttpStatus.BAD_REQUEST, falla(() -> servicio.registrar(sinApellido)).getEstado());
        RegistrarCompromisoRequest correoMalo = new RegistrarCompromisoRequest(5, "12.345.678-5", "Ana", "Paz",
                "x", "+56912345678", "Arriendo", 1000, HOY, null, null, HOY, AHORA);
        assertEquals("El correo no es válido.", falla(() -> servicio.registrar(correoMalo)).getMessage());
        RegistrarCompromisoRequest correoNulo = new RegistrarCompromisoRequest(5, "12.345.678-5", "Ana", "Paz",
                null, "+56912345678", "Arriendo", 1000, HOY, null, null, HOY, AHORA);
        assertEquals("El correo no es válido.", falla(() -> servicio.registrar(correoNulo)).getMessage());
        RegistrarCompromisoRequest telMalo = new RegistrarCompromisoRequest(5, "12.345.678-5", "Ana", "Paz",
                "a@b.cl", "12", "Arriendo", 1000, HOY, null, null, HOY, AHORA);
        assertEquals("El teléfono no es válido.", falla(() -> servicio.registrar(telMalo)).getMessage());
        RegistrarCompromisoRequest telNulo = new RegistrarCompromisoRequest(5, "12.345.678-5", "Ana", "Paz",
                "a@b.cl", null, "Arriendo", 1000, HOY, null, null, HOY, AHORA);
        assertEquals(HttpStatus.BAD_REQUEST, falla(() -> servicio.registrar(telNulo)).getEstado());
    }

    @Test
    void registrarRechazaRutDePersonalOInactivo() {
        when(auth.porRut("12.345.678-5")).thenReturn(Optional.of(usuario(8, 2, null)));
        assertEquals(HttpStatus.CONFLICT, falla(() -> servicio.registrar(registro("12.345.678-5", HOY, null, null))).getEstado());
        UsuarioRemoto inactivo = new UsuarioRemoto(9, 1, "12.345.678-5", "A", "B", "a@b.cl", "+56912345678", false, null);
        when(auth.porRut("12.345.678-5")).thenReturn(Optional.of(inactivo));
        assertEquals(HttpStatus.CONFLICT, falla(() -> servicio.registrar(registro("12.345.678-5", HOY, null, null))).getEstado());
    }

    @Test
    void registrarSoloLoHaceUnEjecutivoActivoConEmpresa() {
        RegistrarCompromisoRequest comoAgente = new RegistrarCompromisoRequest(1, "12.345.678-5", "A", "B", "a@b.cl",
                "+56912345678", "Arriendo", 1000, HOY, null, null, HOY, AHORA);
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.registrar(comoAgente)).getEstado());
        RegistrarCompromisoRequest sinEjecutivo = new RegistrarCompromisoRequest(null, "12.345.678-5", "A", "B", "a@b.cl",
                "+56912345678", "Arriendo", 1000, HOY, null, null, HOY, AHORA);
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.registrar(sinEjecutivo)).getEstado());
        when(auth.usuario(6)).thenReturn(Optional.of(usuario(6, 3, null)));
        RegistrarCompromisoRequest sinEmpresa = new RegistrarCompromisoRequest(6, "12.345.678-5", "A", "B", "a@b.cl",
                "+56912345678", "Arriendo", 1000, HOY, null, null, HOY, AHORA);
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.registrar(sinEmpresa)).getEstado());
    }

    @Test
    void registrarValidaLosCamposDelCompromiso() {
        assertEquals("El RUT no es válido.", falla(() -> servicio.registrar(registro("1-8", HOY, null, null))).getMessage());
        assertEquals("El RUT no es válido.", falla(() -> servicio.registrar(registro(null, HOY, null, null))).getMessage());
        assertTrue(falla(() -> servicio.registrar(new RegistrarCompromisoRequest(5, "12.345.678-5", "A", "B", "a@b.cl",
                "+56912345678", " ", 1000, HOY, null, null, HOY, AHORA))).getMessage().startsWith("Escribe el concepto"));
        assertTrue(falla(() -> servicio.registrar(new RegistrarCompromisoRequest(5, "12.345.678-5", "A", "B", "a@b.cl",
                "+56912345678", "x".repeat(121), 1000, HOY, null, null, HOY, AHORA))).getMessage().startsWith("Escribe el concepto"));
        assertEquals("El monto debe ser mayor que cero.", falla(() -> servicio.registrar(new RegistrarCompromisoRequest(5,
                "12.345.678-5", "A", "B", "a@b.cl", "+56912345678", "Arriendo", 0, HOY, null, null, HOY, AHORA))).getMessage());
        assertEquals("Indica la fecha de vencimiento.", falla(() -> servicio.registrar(new RegistrarCompromisoRequest(5,
                "12.345.678-5", "A", "B", "a@b.cl", "+56912345678", "Arriendo", 1000, null, null, null, HOY, AHORA))).getMessage());
        assertTrue(falla(() -> servicio.registrar(registro("12.345.678-5", HOY, 1, null))).getMessage().startsWith("Indica el número"));
        assertEquals("La cuota no puede ser mayor que el total de cuotas.",
                falla(() -> servicio.registrar(registro("12.345.678-5", HOY, 4, 3))).getMessage());
        assertEquals("La fecha de vencimiento no puede ser anterior a hoy.",
                falla(() -> servicio.registrar(registro("12.345.678-5", HOY.minusDays(1), null, null))).getMessage());
    }

    @Test
    void registrarSinFechasUsaElRelojDelServidor() {
        when(auth.porRut("12.345.678-5")).thenReturn(Optional.of(usuario(1, 1, null)));
        RegistrarCompromisoRequest sinFechas = new RegistrarCompromisoRequest(5, "12.345.678-5", "A", "B", "a@b.cl",
                "+56912345678", "Arriendo", 1000, LocalDate.of(2026, 12, 1), null, null, null, null);
        Compromiso c = servicio.registrar(sinFechas);
        assertEquals(AHORA, c.getFechaRegistro());
        assertEquals("CP-2026-0001", c.getFolio());
    }

    @Test
    void registrarDuplicadoDa409() {
        when(auth.porRut("12.345.678-5")).thenReturn(Optional.of(usuario(1, 1, null)));
        when(compromisos.existsByIdUsuarioFinalAndIdEmpresaAndConceptoAndNumeroCuota(1, 7, "Cuota arriendo", 1))
                .thenReturn(true);
        assertEquals("Este compromiso ya está registrado para esa persona.",
                falla(() -> servicio.registrar(registro("12.345.678-5", HOY, 1, 3))).getMessage());
    }

    private AnularRequest anular(Integer ejecutivo, Integer motivo, String comentario) {
        return new AnularRequest(ejecutivo, motivo, comentario, AHORA);
    }

    @Test
    void anularUnCompromisoDeLaEmpresaDelEjecutivo() {
        pendiente.setIdEmpresa(7);
        Anulacion a = servicio.anular(10, anular(5, 1, "  Repactado  "));
        assertEquals("Repactado", a.getComentario());
        assertEquals(ReglasPagos.ANULADO, pendiente.getIdEstadoCompromiso());
        assertEquals(AHORA, a.getFecha());
    }

    @Test
    void anularSinComentarioGuardaNulo() {
        pendiente.setIdEmpresa(7);
        assertNull(servicio.anular(10, anular(5, 1, null)).getComentario());
    }

    @Test
    void anularExigeEjecutivoYEmpresaCorrecta() {
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.anular(10, anular(1, 1, "x"))).getEstado());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.anular(10, anular(null, 1, "x"))).getEstado());
        assertEquals("Solo puedes anular compromisos de tu empresa.", falla(() -> servicio.anular(10, anular(5, 1, "x"))).getMessage());
        assertEquals(HttpStatus.FORBIDDEN, falla(() -> servicio.anular(99, anular(5, 1, "x"))).getEstado());
    }

    @Test
    void anularNoSePuedeSiEstaPagadoOYaAnulado() {
        pendiente.setIdEmpresa(7);
        pendiente.setIdEstadoCompromiso(ReglasPagos.PAGADO);
        assertEquals("Un compromiso pagado no se puede anular.", falla(() -> servicio.anular(10, anular(5, 1, "x"))).getMessage());
        pendiente.setIdEstadoCompromiso(ReglasPagos.ANULADO);
        assertEquals("Este compromiso ya está anulado.", falla(() -> servicio.anular(10, anular(5, 1, "x"))).getMessage());
    }

    @Test
    void anularValidaMotivoYComentario() {
        pendiente.setIdEmpresa(7);
        assertEquals("Elige el motivo de la anulación.", falla(() -> servicio.anular(10, anular(5, null, "x"))).getMessage());
        when(catalogos.existsByTipoAndCodigo(ReglasPagos.TIPO_MOTIVO_ANULACION, 50)).thenReturn(false);
        assertEquals(HttpStatus.BAD_REQUEST, falla(() -> servicio.anular(10, anular(5, 50, "x"))).getEstado());
        assertTrue(falla(() -> servicio.anular(10, anular(5, 4, " "))).getMessage().contains("«Otro»"));
        assertTrue(falla(() -> servicio.anular(10, anular(5, 1, "x".repeat(256)))).getMessage().contains("255"));
    }

    @Test
    void actualizarVencimientosAplicaElRecargoDeLaEmpresa() {
        Compromiso vencido = compromiso(20, 1, ReglasPagos.PENDIENTE, HOY.minusDays(3));
        EmpresaCliente empresa = new EmpresaCliente();
        empresa.setRecargoAtraso(1_500);
        when(compromisos.findByIdEstadoCompromisoAndFechaVencimientoBefore(ReglasPagos.PENDIENTE, HOY))
                .thenReturn(List.of(vencido));
        when(empresas.findById(1)).thenReturn(Optional.of(empresa));
        assertEquals(1, servicio.actualizarVencimientos(new VencimientosRequest(HOY, AHORA)));
        assertEquals(ReglasPagos.VENCIDO, vencido.getIdEstadoCompromiso());
        assertEquals(1_500, vencido.getRecargoAplicado());
    }

    @Test
    void actualizarVencimientosSinEmpresaNiFechasUsaCeroYElReloj() {
        Compromiso vencido = compromiso(20, 1, ReglasPagos.PENDIENTE, HOY.minusDays(3));
        when(compromisos.findByIdEstadoCompromisoAndFechaVencimientoBefore(ReglasPagos.PENDIENTE, LocalDate.of(2026, 10, 6)))
                .thenReturn(List.of(vencido));
        when(empresas.findById(1)).thenReturn(Optional.empty());
        assertEquals(1, servicio.actualizarVencimientos(new VencimientosRequest(null, null)));
        assertEquals(0, vencido.getRecargoAplicado());
    }
}
