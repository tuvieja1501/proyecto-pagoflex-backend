package cl.duoc.pagoflex.auth.validacion;

import java.util.regex.Pattern;

public final class Validaciones {

    public static final int MIN_CLAVE = 8;
    public static final int MAX_CLAVE = 40;
    public static final int MAX_NOMBRE = 60;
    public static final int LARGO_CODIGO = 6;

    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9 ]{8,16}$");
    private static final Pattern NOMBRE = Pattern.compile("^[\\p{L}][\\p{L} .'-]*$");

    private Validaciones() {
    }

    public static boolean correoValido(String texto) {
        return texto != null && CORREO.matcher(texto.trim()).matches();
    }

    public static boolean telefonoValido(String texto) {
        return texto != null && TELEFONO.matcher(texto.trim()).matches();
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    public static String errorCorreo(String texto) {
        if (vacio(texto)) {
            return "Escribe tu correo.";
        }
        if (!correoValido(texto)) {
            return "Revisa el correo. Debe verse como nombre@correo.cl";
        }
        return null;
    }

    public static String errorRut(String texto) {
        if (vacio(texto)) {
            return "Escribe tu RUT.";
        }
        if (!RutUtil.esValido(texto)) {
            return "El RUT no es válido. Ejemplo: 12.345.678-5";
        }
        return null;
    }

    public static String errorIdentificador(String texto) {
        if (vacio(texto)) {
            return "Escribe tu correo o tu RUT.";
        }
        if (texto.contains("@")) {
            return correoValido(texto) ? null : "Revisa el correo. Debe verse como nombre@correo.cl";
        }
        if (RutUtil.esValido(texto)) {
            return null;
        }
        return "Escribe un correo válido o un RUT válido (12.345.678-5).";
    }

    public static String errorNombre(String texto, String etiqueta) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            return "Escribe " + etiqueta + ".";
        }
        if (limpio.length() > MAX_NOMBRE) {
            return "Usa hasta " + MAX_NOMBRE + " letras.";
        }
        if (!NOMBRE.matcher(limpio).matches()) {
            return "Usa solo letras en " + etiqueta + ".";
        }
        return null;
    }

    public static String errorTelefono(String texto, boolean obligatorio) {
        if (vacio(texto)) {
            return obligatorio ? "Escribe tu teléfono." : null;
        }
        if (!telefonoValido(texto)) {
            return "Revisa el teléfono. Ejemplo: +56 9 1234 5678";
        }
        return null;
    }

    public static String errorClaveNueva(String clave) {
        if (clave == null || clave.isEmpty()) {
            return "Escribe una clave.";
        }
        if (clave.length() < MIN_CLAVE) {
            return "Usa al menos " + MIN_CLAVE + " caracteres.";
        }
        if (clave.length() > MAX_CLAVE) {
            return "Usa hasta " + MAX_CLAVE + " caracteres.";
        }
        if (clave.chars().anyMatch(Character::isWhitespace)) {
            return "La clave no puede tener espacios.";
        }
        if (clave.chars().noneMatch(Character::isLetter)) {
            return "Agrega al menos una letra.";
        }
        if (clave.chars().noneMatch(Character::isDigit)) {
            return "Agrega al menos un número.";
        }
        return null;
    }

    public static String errorCodigo(String texto) {
        if (vacio(texto)) {
            return "Escribe el código que te enviamos.";
        }
        if (texto.length() != LARGO_CODIGO || !texto.chars().allMatch(Character::isDigit)) {
            return "El código tiene " + LARGO_CODIGO + " números.";
        }
        return null;
    }

    public static String primerErrorRegistro(String rut, String nombres, String apellidos, String correo,
                                             String telefono, Integer idComuna, String clave) {
        String[] errores = {
                errorRut(rut),
                errorNombre(nombres, "tus nombres"),
                errorNombre(apellidos, "tus apellidos"),
                errorCorreo(correo),
                errorTelefono(telefono, true),
                idComuna == null ? "Elige tu comuna." : null,
                errorClaveNueva(clave)
        };
        for (String error : errores) {
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    public static String primerErrorPerfil(String nombres, String apellidos, String correo, String telefono,
                                           Integer idComuna, boolean requiereComuna) {
        String[] errores = {
                errorNombre(nombres, "tus nombres"),
                errorNombre(apellidos, "tus apellidos"),
                errorCorreo(correo),
                errorTelefono(telefono, requiereComuna),
                requiereComuna && idComuna == null ? "Elige tu comuna." : null
        };
        for (String error : errores) {
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    public static String primerErrorCambioClave(String actual, String nueva) {
        if (actual == null || actual.isEmpty()) {
            return "Escribe tu clave actual.";
        }
        String error = errorClaveNueva(nueva);
        if (error != null) {
            return error;
        }
        if (nueva.equals(actual)) {
            return "La nueva clave debe ser distinta a la actual.";
        }
        return null;
    }
}
