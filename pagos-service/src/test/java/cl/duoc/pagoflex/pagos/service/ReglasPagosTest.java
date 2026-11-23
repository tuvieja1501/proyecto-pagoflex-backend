package cl.duoc.pagoflex.pagos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReglasPagosTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 6);

    @Test
    void soloSePuedePagarPendienteOVencido() {
        assertTrue(ReglasPagos.puedePagar(ReglasPagos.PENDIENTE));
        assertTrue(ReglasPagos.puedePagar(ReglasPagos.VENCIDO));
        assertFalse(ReglasPagos.puedePagar(ReglasPagos.PAGADO));
        assertFalse(ReglasPagos.puedePagar(ReglasPagos.EN_REVISION));
        assertFalse(ReglasPagos.puedePagar(ReglasPagos.ANULADO));
        assertTrue(ReglasPagos.puedeReportar(ReglasPagos.VENCIDO));
        assertFalse(ReglasPagos.puedeReportar(ReglasPagos.PAGADO));
    }

    @Test
    void anularNoSePuedeSiYaEstaPagadoOAnulado() {
        assertTrue(ReglasPagos.puedeAnular(ReglasPagos.PENDIENTE));
        assertTrue(ReglasPagos.puedeAnular(ReglasPagos.EN_REVISION));
        assertFalse(ReglasPagos.puedeAnular(ReglasPagos.PAGADO));
        assertFalse(ReglasPagos.puedeAnular(ReglasPagos.ANULADO));
    }

    @Test
    void estadoTrasResolverDependeDeLaFechaDeVencimiento() {
        assertEquals(ReglasPagos.VENCIDO,
                ReglasPagos.estadoTrasResolver(ReglasPagos.EN_REVISION, HOY.minusDays(1), HOY));
        assertEquals(ReglasPagos.PENDIENTE,
                ReglasPagos.estadoTrasResolver(ReglasPagos.EN_REVISION, HOY, HOY));
        assertEquals(ReglasPagos.PAGADO,
                ReglasPagos.estadoTrasResolver(ReglasPagos.PAGADO, HOY.minusDays(5), HOY));
        assertEquals(ReglasPagos.ANULADO,
                ReglasPagos.estadoTrasResolver(ReglasPagos.ANULADO, HOY.minusDays(5), HOY));
    }

    @Test
    void numerosCorrelativos() {
        assertEquals("CPR-000001", ReglasPagos.siguienteNumero(null, "CPR-", "%06d"));
        assertEquals("CPR-000010", ReglasPagos.siguienteNumero("CPR-000009", "CPR-", "%06d"));
        assertEquals("REP-0001", ReglasPagos.siguienteNumero("basura", "REP-", "%04d"));
        assertEquals("REP-0001", ReglasPagos.siguienteNumero("REP-xx", "REP-", "%04d"));
    }
}
