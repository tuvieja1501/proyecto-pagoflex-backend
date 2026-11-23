package cl.duoc.pagoflex.recomendaciones.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RutUtilTest {

    @Test
    void limpiaPuntosGuionYEspacios() {
        assertEquals("12345678K", RutUtil.limpiar("12.345.678-k "));
    }

    @Test
    void aceptaRutsConDigitoVerificadorCorrecto() {
        assertTrue(RutUtil.esValido("12.345.678-5"));
        assertTrue(RutUtil.esValido("1-9"));
        assertTrue(RutUtil.esValido("6-k"));
        assertTrue(RutUtil.esValido("14-0"));
    }

    @Test
    void rechazaRutsInvalidos() {
        assertFalse(RutUtil.esValido("12.345.678-9"));
        assertFalse(RutUtil.esValido("5"));
        assertFalse(RutUtil.esValido("ab.cde.fgh-1"));
        assertFalse(RutUtil.esValido("123.456.789-0"));
    }

    @Test
    void calculaElDigitoVerificadorEnTodosLosCasos() {
        assertEquals('5', RutUtil.dvEsperado("12345678"));
        assertEquals('9', RutUtil.dvEsperado("1"));
        assertEquals('K', RutUtil.dvEsperado("6"));
        assertEquals('0', RutUtil.dvEsperado("14"));
    }

    @Test
    void formateaConPuntosYGuion() {
        assertEquals("12.345.678-5", RutUtil.formatear("123456785"));
        assertEquals("1-9", RutUtil.formatear("19"));
        assertEquals("x", RutUtil.formatear("x"));
    }
}
