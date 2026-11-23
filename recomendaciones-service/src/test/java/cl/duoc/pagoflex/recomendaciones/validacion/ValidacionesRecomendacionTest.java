package cl.duoc.pagoflex.recomendaciones.validacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidacionesRecomendacionTest {

    private String error(String org, Integer rubro, Integer comuna, String rut, String cn, String cc, String ct,
                         boolean autoriza) {
        return ValidacionesRecomendacion.primerError(org, rubro, comuna, rut, cn, cc, ct, autoriza);
    }

    @Test
    void unaRecomendacionMinimaEsValida() {
        assertNull(error("Gimnasio Sur", 1, 1, "", "", "", "", false));
    }

    @Test
    void validaElNombreDeLaOrganizacion() {
        assertEquals("Escribe el nombre de la organización.", error(null, 1, 1, "", "", "", "", false));
        assertEquals("Escribe el nombre de la organización.", error("  ", 1, 1, "", "", "", "", false));
        assertEquals("El nombre es muy corto.", error("A", 1, 1, "", "", "", "", false));
        assertEquals("Usa hasta 80 letras.", error("x".repeat(81), 1, 1, "", "", "", "", false));
    }

    @Test
    void exigeRubroYComuna() {
        assertEquals("Elige el rubro.", error("Club", null, 1, "", "", "", "", false));
        assertEquals("Elige la comuna.", error("Club", 1, null, "", "", "", "", false));
    }

    @Test
    void validaElRutDeLaOrganizacionSoloSiSeEscribe() {
        assertEquals("El RUT de la organización no es válido.", error("Club", 1, 1, "12.345.678-9", "", "", "", false));
        assertNull(error("Club", 1, 1, "12.345.678-5", "", "", "", false));
        assertNull(error("Club", 1, 1, null, null, null, null, false));
    }

    @Test
    void elContactoExigeNombreYUnMedio() {
        assertEquals("Escribe el nombre de la persona de contacto.", error("Club", 1, 1, "", "", "a@b.cl", "", true));
        assertEquals("Usa hasta 60 letras.", error("Club", 1, 1, "", "x".repeat(61), "a@b.cl", "", true));
        assertEquals("Agrega un correo o un teléfono de contacto.", error("Club", 1, 1, "", "Ana", "", "", true));
    }

    @Test
    void elContactoValidaCorreoTelefonoYAutorizacion() {
        assertEquals("El correo de contacto no es válido.", error("Club", 1, 1, "", "Ana", "malo", "", true));
        assertEquals("El teléfono de contacto no es válido.", error("Club", 1, 1, "", "Ana", "", "12", true));
        assertTrue(error("Club", 1, 1, "", "Ana", "a@b.cl", "", false).startsWith("Para guardar estos datos"));
        assertNull(error("Club", 1, 1, "", "Ana", "a@b.cl", "+56 9 1234 5678", true));
        assertNull(error("Club", 1, 1, "", "Ana", "", "+56912345678", true));
    }

    @Test
    void correoYTelefonoSeValidanPorSeparado() {
        assertTrue(ValidacionesRecomendacion.correoValido(" ana@correo.cl "));
        assertFalse(ValidacionesRecomendacion.correoValido("ana@"));
        assertFalse(ValidacionesRecomendacion.correoValido(null));
        assertTrue(ValidacionesRecomendacion.telefonoValido("+56 9 1234 5678"));
        assertFalse(ValidacionesRecomendacion.telefonoValido("abc"));
        assertFalse(ValidacionesRecomendacion.telefonoValido(null));
    }
}
