package cl.duoc.pagoflex.recomendaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReglasRecomendacionTest {

    @Test
    void soloSeUnioYDescartadaSonFinales() {
        assertTrue(ReglasRecomendacion.esFinal(ReglasRecomendacion.SE_UNIO));
        assertTrue(ReglasRecomendacion.esFinal(ReglasRecomendacion.DESCARTADA));
        assertFalse(ReglasRecomendacion.esFinal(ReglasRecomendacion.RECIBIDA));
        assertFalse(ReglasRecomendacion.esFinal(ReglasRecomendacion.EN_EVALUACION));
    }

    @Test
    void cadaEstadoTieneSuSiguienteEtapa() {
        assertEquals(List.of(2), ReglasRecomendacion.estadosSiguientes(ReglasRecomendacion.RECIBIDA));
        assertEquals(List.of(3), ReglasRecomendacion.estadosSiguientes(ReglasRecomendacion.CONTACTADA));
        assertEquals(List.of(4, 5), ReglasRecomendacion.estadosSiguientes(ReglasRecomendacion.EN_EVALUACION));
        assertTrue(ReglasRecomendacion.estadosSiguientes(ReglasRecomendacion.SE_UNIO).isEmpty());
        assertTrue(ReglasRecomendacion.estadosSiguientes(99).isEmpty());
    }

    @Test
    void noSePuedeSaltarEtapas() {
        assertTrue(ReglasRecomendacion.puedeCambiar(1, 2));
        assertFalse(ReglasRecomendacion.puedeCambiar(1, 3));
        assertFalse(ReglasRecomendacion.puedeCambiar(2, 4));
        assertTrue(ReglasRecomendacion.puedeCambiar(3, 4));
        assertTrue(ReglasRecomendacion.puedeCambiar(3, 5));
        assertFalse(ReglasRecomendacion.puedeCambiar(4, 5));
    }

    @Test
    void normalizaElNombre() {
        assertEquals("Club Los Halcones", ReglasRecomendacion.normalizarNombre("  Club   Los  Halcones "));
    }

    @Test
    void numeraCorrelativamente() {
        assertEquals("REC-0001", ReglasRecomendacion.siguienteNumero(null));
        assertEquals("REC-0007", ReglasRecomendacion.siguienteNumero("REC-0006"));
        assertEquals("REC-0001", ReglasRecomendacion.siguienteNumero("REC-xx"));
        assertEquals("REC-0001", ReglasRecomendacion.siguienteNumero("OTRO-5"));
    }
}
