package cl.duoc.pagoflex.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

    public static final int ITERACIONES = 10_000;
    public static final int LARGO_CODIGO = 6;
    private static final int BITS_CLAVE = 256;
    private static final int BYTES_SAL = 16;
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final char[] DIGITOS = "0123456789abcdef".toCharArray();

    private final SecureRandom azar = new SecureRandom();

    public String nuevaSal() {
        byte[] bytes = new byte[BYTES_SAL];
        azar.nextBytes(bytes);
        return aHex(bytes);
    }

    public String hash(String clave, String sal) {
        PBEKeySpec especificacion = new PBEKeySpec(clave.toCharArray(), desdeHex(sal), ITERACIONES, BITS_CLAVE);
        try {
            return aHex(SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la clave", e);
        } finally {
            especificacion.clearPassword();
        }
    }

    public boolean verificar(String clave, String sal, String hashEsperado) {
        return MessageDigest.isEqual(
                hash(clave, sal).getBytes(StandardCharsets.UTF_8),
                hashEsperado.getBytes(StandardCharsets.UTF_8));
    }

    public String codigoRecuperacion() {
        int numero = azar.nextInt(1_000_000);
        return String.format("%0" + LARGO_CODIGO + "d", numero);
    }

    private static String aHex(byte[] bytes) {
        StringBuilder salida = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int v = b & 0xFF;
            salida.append(DIGITOS[v >> 4]).append(DIGITOS[v & 0x0F]);
        }
        return salida.toString();
    }

    private static byte[] desdeHex(String texto) {
        byte[] bytes = new byte[texto.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(texto.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
