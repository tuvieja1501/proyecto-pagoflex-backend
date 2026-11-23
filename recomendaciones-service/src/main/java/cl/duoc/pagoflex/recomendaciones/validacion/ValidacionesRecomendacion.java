package cl.duoc.pagoflex.recomendaciones.validacion;

import cl.duoc.pagoflex.recomendaciones.util.RutUtil;
import java.util.regex.Pattern;

public final class ValidacionesRecomendacion {

    public static final int MAX_ORGANIZACION = 80;
    public static final int MAX_NOMBRE = 60;

    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9 ]{8,16}$");

    private ValidacionesRecomendacion() {
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    public static boolean correoValido(String texto) {
        return texto != null && CORREO.matcher(texto.trim()).matches();
    }

    public static boolean telefonoValido(String texto) {
        return texto != null && TELEFONO.matcher(texto.trim()).matches();
    }

    public static String primerError(String organizacion, Integer idRubro, Integer idComuna, String rut,
                                     String contactoNombre, String contactoCorreo, String contactoTelefono,
                                     boolean autoriza) {
        String limpio = organizacion == null ? "" : organizacion.trim();
        if (limpio.isEmpty()) {
            return "Escribe el nombre de la organización.";
        }
        if (limpio.length() < 2) {
            return "El nombre es muy corto.";
        }
        if (limpio.length() > MAX_ORGANIZACION) {
            return "Usa hasta " + MAX_ORGANIZACION + " letras.";
        }
        if (idRubro == null) {
            return "Elige el rubro.";
        }
        if (idComuna == null) {
            return "Elige la comuna.";
        }
        if (!vacio(rut) && !RutUtil.esValido(rut)) {
            return "El RUT de la organización no es válido.";
        }
        boolean hayContacto = !vacio(contactoNombre) || !vacio(contactoCorreo) || !vacio(contactoTelefono);
        if (!hayContacto) {
            return null;
        }
        if (vacio(contactoNombre)) {
            return "Escribe el nombre de la persona de contacto.";
        }
        if (contactoNombre.trim().length() > MAX_NOMBRE) {
            return "Usa hasta " + MAX_NOMBRE + " letras.";
        }
        if (vacio(contactoCorreo) && vacio(contactoTelefono)) {
            return "Agrega un correo o un teléfono de contacto.";
        }
        if (!vacio(contactoCorreo) && !correoValido(contactoCorreo)) {
            return "El correo de contacto no es válido.";
        }
        if (!vacio(contactoTelefono) && !telefonoValido(contactoTelefono)) {
            return "El teléfono de contacto no es válido.";
        }
        if (!autoriza) {
            return "Para guardar estos datos, confirma que la persona autorizó que PagoFlex la contacte.";
        }
        return null;
    }
}
