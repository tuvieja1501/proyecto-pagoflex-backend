package cl.duoc.pagoflex.auth.validacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidacionesTest {

    @Test
    void rutValidoEInvalido() {
        assertTrue(RutUtil.esValido("12.345.678-5"));
        assertFalse(RutUtil.esValido("12.345.678-9"));
        assertFalse(RutUtil.esValido("5"));
        assertFalse(RutUtil.esValido("123456789-1"));
        assertFalse(RutUtil.esValido("12A45678-5"));
        assertTrue(RutUtil.esValido("11.111.111-1"));
    }

    @Test
    void digitoVerificadorKyCero() {
        assertEquals('K', RutUtil.dvEsperado("6"));
        assertEquals('0', RutUtil.dvEsperado("0"));
    }

    @Test
    void formateaRut() {
        assertEquals("12.345.678-5", RutUtil.formatear("123456785"));
        assertEquals("1.234-5", RutUtil.formatear("12345"));
        assertEquals("5", RutUtil.formatear("5"));
    }

    @Test
    void correoYTelefono() {
        assertTrue(Validaciones.correoValido("camila@correo.cl"));
        assertFalse(Validaciones.correoValido("camila"));
        assertFalse(Validaciones.correoValido(null));
        assertTrue(Validaciones.telefonoValido("+56 9 1234 5678"));
        assertFalse(Validaciones.telefonoValido("123"));
        assertFalse(Validaciones.telefonoValido(null));
    }

    @Test
    void mensajesDeCorreoRutEIdentificador() {
        assertEquals("Escribe tu correo.", Validaciones.errorCorreo(" "));
        assertEquals("Revisa el correo. Debe verse como nombre@correo.cl", Validaciones.errorCorreo("x"));
        assertNull(Validaciones.errorCorreo("a@b.cl"));
        assertEquals("Escribe tu RUT.", Validaciones.errorRut(null));
        assertEquals("El RUT no es válido. Ejemplo: 12.345.678-5", Validaciones.errorRut("1-8"));
        assertNull(Validaciones.errorRut("12.345.678-5"));
        assertEquals("Escribe tu correo o tu RUT.", Validaciones.errorIdentificador(""));
        assertEquals("Revisa el correo. Debe verse como nombre@correo.cl", Validaciones.errorIdentificador("a@b"));
        assertNull(Validaciones.errorIdentificador("a@b.cl"));
        assertNull(Validaciones.errorIdentificador("12.345.678-5"));
        assertEquals("Escribe un correo válido o un RUT válido (12.345.678-5).", Validaciones.errorIdentificador("hola"));
    }

    @Test
    void nombresYTelefono() {
        assertEquals("Escribe tus nombres.", Validaciones.errorNombre("  ", "tus nombres"));
        assertEquals("Escribe tus nombres.", Validaciones.errorNombre(null, "tus nombres"));
        assertEquals("Usa hasta 60 letras.", Validaciones.errorNombre("a".repeat(61), "tus nombres"));
        assertEquals("Usa solo letras en tus nombres.", Validaciones.errorNombre("R0jas", "tus nombres"));
        assertNull(Validaciones.errorNombre("María José", "tus nombres"));
        assertEquals("Escribe tu teléfono.", Validaciones.errorTelefono("", true));
        assertNull(Validaciones.errorTelefono("", false));
        assertEquals("Revisa el teléfono. Ejemplo: +56 9 1234 5678", Validaciones.errorTelefono("12", false));
        assertNull(Validaciones.errorTelefono("+56912345678", true));
    }

    @Test
    void reglasDeLaClaveNueva() {
        assertEquals("Escribe una clave.", Validaciones.errorClaveNueva(""));
        assertEquals("Escribe una clave.", Validaciones.errorClaveNueva(null));
        assertEquals("Usa al menos 8 caracteres.", Validaciones.errorClaveNueva("abc12"));
        assertEquals("Usa hasta 40 caracteres.", Validaciones.errorClaveNueva("a1".repeat(21)));
        assertEquals("La clave no puede tener espacios.", Validaciones.errorClaveNueva("abc 12345"));
        assertEquals("Agrega al menos una letra.", Validaciones.errorClaveNueva("12345678"));
        assertEquals("Agrega al menos un número.", Validaciones.errorClaveNueva("abcdefgh"));
        assertNull(Validaciones.errorClaveNueva("PagoFlex2026"));
    }

    @Test
    void codigoDeRecuperacion() {
        assertEquals("Escribe el código que te enviamos.", Validaciones.errorCodigo(null));
        assertEquals("El código tiene 6 números.", Validaciones.errorCodigo("12345"));
        assertEquals("El código tiene 6 números.", Validaciones.errorCodigo("12345a"));
        assertNull(Validaciones.errorCodigo("012345"));
    }

    @Test
    void primerErrorDelRegistro() {
        assertNull(Validaciones.primerErrorRegistro("12.345.678-5", "Camila", "Rojas", "c@c.cl", "+56912345678", 1, "PagoFlex2026"));
        assertEquals("El RUT no es válido. Ejemplo: 12.345.678-5",
                Validaciones.primerErrorRegistro("1-8", "Camila", "Rojas", "c@c.cl", "+56912345678", 1, "PagoFlex2026"));
        assertEquals("Elige tu comuna.",
                Validaciones.primerErrorRegistro("12.345.678-5", "Camila", "Rojas", "c@c.cl", "+56912345678", null, "PagoFlex2026"));
    }

    @Test
    void primerErrorDelPerfil() {
        assertNull(Validaciones.primerErrorPerfil("Camila", "Rojas", "c@c.cl", "", null, false));
        assertEquals("Elige tu comuna.", Validaciones.primerErrorPerfil("Camila", "Rojas", "c@c.cl", "+56912345678", null, true));
        assertEquals("Escribe tu teléfono.", Validaciones.primerErrorPerfil("Camila", "Rojas", "c@c.cl", "", 1, true));
    }

    @Test
    void primerErrorDelCambioDeClave() {
        assertEquals("Escribe tu clave actual.", Validaciones.primerErrorCambioClave("", "Nueva12345"));
        assertEquals("Usa al menos 8 caracteres.", Validaciones.primerErrorCambioClave("Actual123", "n1"));
        assertEquals("La nueva clave debe ser distinta a la actual.",
                Validaciones.primerErrorCambioClave("Actual123", "Actual123"));
        assertNull(Validaciones.primerErrorCambioClave("Actual123", "Nueva12345"));
    }
}
