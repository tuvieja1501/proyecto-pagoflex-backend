package cl.duoc.pagoflex.pagos.service;

import static cl.duoc.pagoflex.pagos.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.pagoflex.pagos.dto.CatalogosDto;
import cl.duoc.pagoflex.pagos.dto.OtorgarReconocimientoRequest;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import cl.duoc.pagoflex.pagos.model.Catalogo;
import cl.duoc.pagoflex.pagos.model.EmpresaCliente;
import cl.duoc.pagoflex.pagos.model.UsuarioReconocimiento;
import cl.duoc.pagoflex.pagos.repository.CatalogoRepository;
import cl.duoc.pagoflex.pagos.repository.EmpresaRepository;
import cl.duoc.pagoflex.pagos.repository.ReconocimientoRepository;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconocimientoYCatalogoServiceTest {

    @Mock
    private ReconocimientoRepository reconocimientos;
    @Mock
    private CatalogoRepository catalogos;
    @Mock
    private EmpresaRepository empresas;

    private ReconocimientoService reconocimientoService;
    private CatalogoService catalogoService;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneId.of("UTC"));
        reconocimientoService = new ReconocimientoService(reconocimientos, reloj);
        catalogoService = new CatalogoService(catalogos, empresas);
        when(reconocimientos.save(any(UsuarioReconocimiento.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void otorgarUnLogroNuevo() {
        UsuarioReconocimiento r = reconocimientoService.otorgar(
                new OtorgarReconocimientoRequest(1, 1, AHORA, "  Pagaste a tiempo  "));
        assertEquals("Pagaste a tiempo", r.getMotivo());
        assertEquals(AHORA, r.getFechaObtencion());
    }

    @Test
    void otorgarSinFechaUsaElRelojDelServidor() {
        UsuarioReconocimiento r = reconocimientoService.otorgar(new OtorgarReconocimientoRequest(1, 2, null, "Motivo"));
        assertEquals(AHORA, r.getFechaObtencion());
    }

    @Test
    void unLogroYaObtenidoNoSeDuplicaNiSeModifica() {
        UsuarioReconocimiento previo = new UsuarioReconocimiento(1, 1, 1, AHORA.minusDays(3), "Original");
        when(reconocimientos.findByIdUsuarioFinalAndIdReconocimiento(1, 1)).thenReturn(Optional.of(previo));
        UsuarioReconocimiento r = reconocimientoService.otorgar(new OtorgarReconocimientoRequest(1, 1, AHORA, "Nuevo"));
        assertEquals("Original", r.getMotivo());
        verify(reconocimientos, never()).save(any(UsuarioReconocimiento.class));
    }

    @Test
    void otorgarExigeDatosCompletos() {
        assertThrows(NegocioException.class,
                () -> reconocimientoService.otorgar(new OtorgarReconocimientoRequest(null, 1, AHORA, "x")));
        assertThrows(NegocioException.class,
                () -> reconocimientoService.otorgar(new OtorgarReconocimientoRequest(1, null, AHORA, "x")));
        assertThrows(NegocioException.class,
                () -> reconocimientoService.otorgar(new OtorgarReconocimientoRequest(1, 1, AHORA, " ")));
        assertThrows(NegocioException.class,
                () -> reconocimientoService.otorgar(new OtorgarReconocimientoRequest(1, 1, AHORA, null)));
    }

    @Test
    void listarLosLogrosDeUnaPersona() {
        when(reconocimientos.findByIdUsuarioFinal(1)).thenReturn(List.of(new UsuarioReconocimiento(), new UsuarioReconocimiento()));
        assertEquals(2, reconocimientoService.listar(1).size());
    }

    @Test
    void elCatalogoAgrupaPorTipoYTraeLasEmpresas() {
        when(catalogos.findByTipoOrderByCodigo(ReglasPagos.TIPO_ESTADO_COMPROMISO))
                .thenReturn(List.of(new Catalogo(1, ReglasPagos.TIPO_ESTADO_COMPROMISO, 1, "Pendiente")));
        when(catalogos.findByTipoOrderByCodigo(ReglasPagos.TIPO_MEDIO_PAGO))
                .thenReturn(List.of(new Catalogo(2, ReglasPagos.TIPO_MEDIO_PAGO, 4, "Pago simulado")));
        EmpresaCliente empresa = new EmpresaCliente(1, "EMP-01", "Razón SpA", "Fantasía", "76.000.000-0", 1,
                "a@b.cl", "+56222222222", 1500, true);
        when(empresas.findAll()).thenReturn(List.of(empresa));
        CatalogosDto dto = catalogoService.todos();
        assertEquals("Pendiente", dto.estadosCompromiso().get(0).nombre());
        assertEquals(4, dto.mediosPago().get(0).id());
        assertEquals(0, dto.canales().size());
        assertEquals("Fantasía", dto.empresas().get(0).nombreFantasia());
    }
}
