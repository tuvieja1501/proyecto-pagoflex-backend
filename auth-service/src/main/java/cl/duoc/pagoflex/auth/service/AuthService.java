package cl.duoc.pagoflex.auth.service;

import cl.duoc.pagoflex.auth.dto.CambioClaveRequest;
import cl.duoc.pagoflex.auth.dto.CodigoRecuperacionDto;
import cl.duoc.pagoflex.auth.dto.LoginRequest;
import cl.duoc.pagoflex.auth.dto.RegistroRequest;
import cl.duoc.pagoflex.auth.dto.RestablecerRequest;
import cl.duoc.pagoflex.auth.exception.NegocioException;
import cl.duoc.pagoflex.auth.model.Credencial;
import cl.duoc.pagoflex.auth.model.Usuario;
import cl.duoc.pagoflex.auth.repository.ComunaRepository;
import cl.duoc.pagoflex.auth.repository.CredencialRepository;
import cl.duoc.pagoflex.auth.repository.UsuarioRepository;
import cl.duoc.pagoflex.auth.security.PasswordHasher;
import cl.duoc.pagoflex.auth.validacion.RutUtil;
import cl.duoc.pagoflex.auth.validacion.Validaciones;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public static final int MAX_INTENTOS = 5;
    public static final long MINUTOS_BLOQUEO = 5;
    public static final long MINUTOS_CODIGO = 10;
    public static final int ROL_USUARIO_FINAL = 1;

    private static final String CREDENCIALES_INVALIDAS =
            "El correo, el RUT o la clave no coinciden. Revisa e intenta de nuevo.";
    private static final String SIN_CLAVE =
            "Esta cuenta aún no tiene clave. Toca «Crear cuenta» e ingresa tu RUT para activarla.";
    private static final String SIN_CUENTA = "No encontramos una cuenta con ese correo.";

    private final UsuarioRepository usuarios;
    private final CredencialRepository credenciales;
    private final ComunaRepository comunas;
    private final PasswordHasher hasher;
    private final Clock reloj;

    public AuthService(UsuarioRepository usuarios, CredencialRepository credenciales, ComunaRepository comunas,
                       PasswordHasher hasher, Clock reloj) {
        this.usuarios = usuarios;
        this.credenciales = credenciales;
        this.comunas = comunas;
        this.hasher = hasher;
        this.reloj = reloj;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private Credencial nuevaCredencial(Integer idUsuario, String clave) {
        String sal = hasher.nuevaSal();
        Credencial c = new Credencial();
        c.setIdUsuario(idUsuario);
        c.setSal(sal);
        c.setClaveHash(hasher.hash(clave, sal));
        c.setIntentosFallidos(0);
        c.setFechaCambio(ahora());
        return c;
    }

    @Transactional(noRollbackFor = NegocioException.class)
    public Usuario iniciarSesion(LoginRequest peticion) {
        String errorIdentificador = Validaciones.errorIdentificador(peticion.identificador());
        if (errorIdentificador != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, errorIdentificador);
        }
        if (peticion.clave() == null || peticion.clave().isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Escribe tu clave.");
        }
        String texto = peticion.identificador().trim();
        Usuario usuario = (texto.contains("@")
                ? usuarios.findByCorreoIgnoreCase(texto)
                : usuarios.findByRut(RutUtil.formatear(texto)))
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS));
        Credencial cred = credenciales.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new NegocioException(HttpStatus.CONFLICT, SIN_CLAVE));
        LocalDateTime ahora = ahora();
        LocalDateTime bloqueo = cred.getBloqueadoHasta();
        if (bloqueo != null && ahora.isBefore(bloqueo)) {
            throw new NegocioException(HttpStatus.LOCKED,
                    String.format("Por seguridad pausamos los intentos un momento. Vuelve a probar a las %02d:%02d.",
                            bloqueo.getHour(), bloqueo.getMinute()));
        }
        if (!hasher.verificar(peticion.clave(), cred.getSal(), cred.getClaveHash())) {
            int intentos = cred.getIntentosFallidos() + 1;
            if (intentos >= MAX_INTENTOS) {
                cred.setIntentosFallidos(0);
                cred.setBloqueadoHasta(ahora.plusMinutes(MINUTOS_BLOQUEO));
                credenciales.save(cred);
                throw new NegocioException(HttpStatus.LOCKED,
                        "Por seguridad pausamos los intentos por " + MINUTOS_BLOQUEO + " minutos. "
                                + "Si no recuerdas tu clave, usa «Olvidé mi clave».");
            }
            cred.setIntentosFallidos(intentos);
            credenciales.save(cred);
            throw new NegocioException(HttpStatus.UNAUTHORIZED,
                    CREDENCIALES_INVALIDAS + " Te quedan " + (MAX_INTENTOS - intentos) + " intento(s).");
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "Esta cuenta está inactiva. Escríbenos para reactivarla.");
        }
        if (cred.getIntentosFallidos() != 0 || cred.getBloqueadoHasta() != null) {
            cred.setIntentosFallidos(0);
            cred.setBloqueadoHasta(null);
            credenciales.save(cred);
        }
        return usuario;
    }

    @Transactional
    public Usuario registrar(RegistroRequest peticion) {
        String error = Validaciones.primerErrorRegistro(peticion.rut(), peticion.nombres(), peticion.apellidos(),
                peticion.correo(), peticion.telefono(), peticion.idComuna(), peticion.clave());
        if (error != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, error);
        }
        if (!comunas.existsById(peticion.idComuna())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Elige tu comuna.");
        }
        String rut = RutUtil.formatear(peticion.rut());
        String correo = peticion.correo().trim();
        String telefono = peticion.telefono().trim();
        Usuario existente = usuarios.findByRut(rut).orElse(null);
        if (existente != null) {
            return activarExistente(existente, peticion, correo, telefono);
        }
        if (usuarios.findByCorreoIgnoreCase(correo).isPresent()) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        }
        Usuario nuevo = new Usuario();
        nuevo.setIdRol(ROL_USUARIO_FINAL);
        nuevo.setRut(rut);
        nuevo.setNombres(peticion.nombres().trim());
        nuevo.setApellidos(peticion.apellidos().trim());
        nuevo.setCorreo(correo);
        nuevo.setTelefono(telefono);
        nuevo.setActivo(true);
        nuevo.setFechaCreacion(ahora());
        nuevo.setIdComuna(peticion.idComuna());
        nuevo.setFechaIncorporacion(LocalDate.now(reloj));
        nuevo.setAvisosActivos(true);
        nuevo.setDiasAnticipacion(3);
        Usuario guardado = usuarios.save(nuevo);
        credenciales.save(nuevaCredencial(guardado.getIdUsuario(), peticion.clave()));
        return guardado;
    }

    private Usuario activarExistente(Usuario existente, RegistroRequest peticion, String correo, String telefono) {
        if (existente.getIdRol() != ROL_USUARIO_FINAL) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Este RUT pertenece a personal de PagoFlex o de una empresa. Ingresa con tu correo y clave.");
        }
        if (credenciales.existsById(existente.getIdUsuario())) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Ya existe una cuenta con este RUT. Ingresa o recupera tu clave.");
        }
        if (!existente.getCorreo().equalsIgnoreCase(correo)) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Una empresa ya te registró con otro correo. Para activar tu cuenta usa ese mismo correo.");
        }
        existente.setTelefono(telefono);
        existente.setIdComuna(peticion.idComuna());
        Usuario guardado = usuarios.save(existente);
        credenciales.save(nuevaCredencial(guardado.getIdUsuario(), peticion.clave()));
        return guardado;
    }

    @Transactional(noRollbackFor = NegocioException.class)
    public CodigoRecuperacionDto solicitarRecuperacion(String correo) {
        String error = Validaciones.errorCorreo(correo);
        if (error != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, error);
        }
        Usuario usuario = usuarios.findByCorreoIgnoreCase(correo.trim())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, SIN_CUENTA));
        Credencial cred = credenciales.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new NegocioException(HttpStatus.CONFLICT, SIN_CLAVE));
        String codigo = hasher.codigoRecuperacion();
        LocalDateTime expira = ahora().plusMinutes(MINUTOS_CODIGO);
        cred.setCodigoHash(hasher.hash(codigo, cred.getSal()));
        cred.setCodigoExpira(expira);
        cred.setIntentosFallidos(0);
        credenciales.save(cred);
        String nombre = usuario.getNombres().split(" ")[0];
        return new CodigoRecuperacionDto(nombre, codigo, expira);
    }

    @Transactional(noRollbackFor = NegocioException.class)
    public void restablecerClave(RestablecerRequest peticion) {
        String errorCodigo = Validaciones.errorCodigo(peticion.codigo());
        if (errorCodigo != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, errorCodigo);
        }
        String errorClave = Validaciones.errorClaveNueva(peticion.nuevaClave());
        if (errorClave != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, errorClave);
        }
        if (peticion.correo() == null) {
            throw new NegocioException(HttpStatus.NOT_FOUND, SIN_CUENTA);
        }
        Usuario usuario = usuarios.findByCorreoIgnoreCase(peticion.correo().trim())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, SIN_CUENTA));
        Credencial cred = credenciales.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new NegocioException(HttpStatus.CONFLICT, "Esta cuenta aún no tiene clave."));
        if (cred.getCodigoHash() == null) {
            throw new NegocioException(HttpStatus.CONFLICT, "Primero pide un código para este correo.");
        }
        LocalDateTime expira = cred.getCodigoExpira();
        if (expira == null || ahora().isAfter(expira)) {
            throw new NegocioException(HttpStatus.GONE, "El código venció. Pide uno nuevo.");
        }
        if (!hasher.verificar(peticion.codigo(), cred.getSal(), cred.getCodigoHash())) {
            int intentos = cred.getIntentosFallidos() + 1;
            if (intentos >= MAX_INTENTOS) {
                cred.setCodigoHash(null);
                cred.setCodigoExpira(null);
                cred.setIntentosFallidos(0);
                credenciales.save(cred);
                throw new NegocioException(HttpStatus.UNAUTHORIZED, "Por seguridad anulamos el código. Pide uno nuevo.");
            }
            cred.setIntentosFallidos(intentos);
            credenciales.save(cred);
            throw new NegocioException(HttpStatus.UNAUTHORIZED,
                    "El código no coincide. Te quedan " + (MAX_INTENTOS - intentos) + " intento(s).");
        }
        credenciales.save(nuevaCredencial(usuario.getIdUsuario(), peticion.nuevaClave()));
    }

    @Transactional(noRollbackFor = NegocioException.class)
    public void cambiarClave(Integer idUsuario, CambioClaveRequest peticion) {
        String error = Validaciones.primerErrorCambioClave(peticion.actual(), peticion.nueva());
        if (error != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, error);
        }
        Credencial cred = credenciales.findById(idUsuario)
                .orElseThrow(() -> new NegocioException(HttpStatus.CONFLICT, "Tu cuenta no tiene clave registrada."));
        if (!hasher.verificar(peticion.actual(), cred.getSal(), cred.getClaveHash())) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Tu clave actual no es correcta.");
        }
        credenciales.save(nuevaCredencial(idUsuario, peticion.nueva()));
    }
}
