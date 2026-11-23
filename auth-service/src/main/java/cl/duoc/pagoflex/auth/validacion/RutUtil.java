package cl.duoc.pagoflex.auth.validacion;

public final class RutUtil {

    private RutUtil() {
    }

    public static String limpiar(String texto) {
        return texto.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
    }

    public static boolean esValido(String texto) {
        String limpio = limpiar(texto);
        if (limpio.length() < 2) {
            return false;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        if (cuerpo.length() > 8 || !cuerpo.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return false;
        }
        return dvEsperado(cuerpo) == dv;
    }

    public static char dvEsperado(String cuerpo) {
        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }

    public static String formatear(String texto) {
        String limpio = limpiar(texto);
        if (limpio.length() < 2) {
            return texto;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        StringBuilder conPuntos = new StringBuilder();
        int contador = 0;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            if (contador > 0 && contador % 3 == 0) {
                conPuntos.append('.');
            }
            conPuntos.append(cuerpo.charAt(i));
            contador++;
        }
        return conPuntos.reverse() + "-" + dv;
    }
}
