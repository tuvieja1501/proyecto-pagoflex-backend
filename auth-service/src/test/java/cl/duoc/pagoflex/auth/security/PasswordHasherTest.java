package cl.duoc.pagoflex.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void laSalTiene32CaracteresHexYCambiaEnCadaLlamada() {
        String a = hasher.nuevaSal();
        assertEquals(32, a.length());
        assertTrue(a.matches("[0-9a-f]+"));
        assertNotEquals(a, hasher.nuevaSal());
    }

    @Test
    void verificaLaClaveCorrectaYRechazaLaIncorrecta() {
        String sal = hasher.nuevaSal();
        String hash = hasher.hash("PagoFlex2026", sal);
        assertEquals(64, hash.length());
        assertTrue(hasher.verificar("PagoFlex2026", sal, hash));
        assertFalse(hasher.verificar("pagoflex2026", sal, hash));
    }

    @Test
    void elHashNoContieneLaClaveEnClaro() {
        String hash = hasher.hash("PagoFlex2026", hasher.nuevaSal());
        assertFalse(hash.contains("PagoFlex2026"));
    }

    @Test
    void elCodigoDeRecuperacionTieneSeisDigitos() {
        for (int i = 0; i < 50; i++) {
            assertTrue(hasher.codigoRecuperacion().matches("\\d{6}"));
        }
    }
}
